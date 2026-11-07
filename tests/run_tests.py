"""Compile the actual bridge against small Android boundary doubles, then execute it.

These are JVM behavior tests, not an Android/Flyme emulator. The release workflow also
compiles the actual source against the Android SDK and official Morphe dependencies.
"""
from pathlib import Path
import shutil
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
BRIDGE = ROOT / "overlay/extensions/music/src/main/java/app/morphe/extension/music/patches/lyrics/session/FlymeStatusBarLyrics.java"

STUBS = {
    "android/os/Bundle.java": """
package android.os;
public class Bundle extends java.util.HashMap<String,Object> {
  public void putBoolean(String k, boolean v) { put(k,v); }
  public boolean getBoolean(String k, boolean d) { return containsKey(k) ? (Boolean)get(k) : d; }
  public <T> T getParcelable(String k) { return (T)get(k); }
}
""",
    "android/app/Notification.java": """
package android.app;
public class Notification implements Cloneable {
  public static int FLAG_ALWAYS_SHOW_TICKER = 0x1000000;
  public static int FLAG_ONLY_UPDATE_TICKER = 0x2000000;
  public static final int FLAG_NO_CLEAR=32, FLAG_ONLY_ALERT_ONCE=8;
  public static final String EXTRA_MEDIA_SESSION="android.mediaSession";
  public int flags; public CharSequence tickerText;
  public Object actions, icon, artwork; public String channel="media";
  public android.os.Bundle extras=new android.os.Bundle();
  public Notification clone() {
    try { Notification n=(Notification)super.clone(); n.extras=new android.os.Bundle(); n.extras.putAll(extras); return n; }
    catch(CloneNotSupportedException e) { throw new AssertionError(e); }
  }
}
""",
    "android/service/notification/StatusBarNotification.java": """
package android.service.notification;
public class StatusBarNotification {
  public String tag; public int id; public android.app.Notification n;
  public StatusBarNotification(String t,int i,android.app.Notification v){ tag=t;id=i;n=v; }
  public android.app.Notification getNotification(){ return n; }
  public String getTag(){return tag;} public int getId(){return id;}
}
""",
    "android/app/NotificationManager.java": """
package android.app;
import android.service.notification.StatusBarNotification;
public class NotificationManager {
  public java.util.List<StatusBarNotification> entries=new java.util.ArrayList<>();
  public int posts; public boolean fail;
  public StatusBarNotification[] getActiveNotifications(){return entries.toArray(new StatusBarNotification[0]);}
  public void notify(String tag,int id,Notification n){
    if(fail)throw new SecurityException("denied");
    posts++;
    for(StatusBarNotification e:entries)if(java.util.Objects.equals(e.tag,tag)&&e.id==id){e.n=n;return;}
    throw new AssertionError("must reuse existing notification identity");
  }
}
""",
    "android/content/Context.java": """
package android.content;
public class Context {
  public static final String NOTIFICATION_SERVICE="notification";
  public android.app.NotificationManager manager=new android.app.NotificationManager();
  public Object getSystemService(String s){return manager;}
}
""",
    "android/media/session/MediaSession.java": """
package android.media.session;
public class MediaSession {
  public static class Token {}
  private final Token token=new Token();
  public Token getSessionToken(){return token;}
}
""",
    "android/media/session/PlaybackState.java": """
package android.media.session;
public class PlaybackState {
  public static final int STATE_PLAYING=3;
  private int state; public PlaybackState(int s){state=s;}
  public int getState(){return state;}
}
""",
    "app/morphe/extension/music/settings/Settings.java": """
package app.morphe.extension.music.settings;
public class Settings {
  public static class Setting { public boolean value=true; public boolean get(){return value;} }
  public static final Setting LYRICS_FLYME_STATUS_BAR=new Setting(),LYRICS_ENABLED=new Setting(),LYRICS_MEDIASESSION=new Setting();
}
""",
    "app/morphe/extension/shared/Utils.java": """
package app.morphe.extension.shared;
public class Utils {
  public static android.content.Context context=new android.content.Context();
  public static android.content.Context getContext(){return context;}
}
""",
    "app/morphe/extension/shared/Logger.java": """
package app.morphe.extension.shared;
public class Logger {
  public static int errors;
  public static void printException(java.util.function.Supplier<String> text,Throwable e){errors++;}
}
""",
    "BridgeTest.java": """
import android.app.*;
import android.media.session.*;
import android.service.notification.*;
import app.morphe.extension.shared.*;
import app.morphe.extension.music.settings.Settings;
import app.morphe.extension.music.patches.lyrics.session.FlymeStatusBarLyrics;

public class BridgeTest {
  static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
  public static void main(String[] args){
    if(args.length>0){
      // Set before bridge class initialization, simulating unavailable OEM fields.
      Notification.FLAG_ALWAYS_SHOW_TICKER=0;
      Notification.FLAG_ONLY_UPDATE_TICKER=0;
    }
    MediaSession session=new MediaSession(), other=new MediaSession();
    NotificationManager manager=Utils.context.manager;
    Notification original=new Notification();
    original.flags=0x4000;
    original.actions=new Object();original.icon=new Object();original.artwork=new Object();
    original.extras.put(Notification.EXTRA_MEDIA_SESSION,session.getSessionToken());
    StatusBarNotification entry=new StatusBarNotification("media",17,original);
    Notification unrelated=new Notification();
    unrelated.extras.put(Notification.EXTRA_MEDIA_SESSION,other.getSessionToken());
    StatusBarNotification foreign=new StatusBarNotification(null,18,unrelated);
    manager.entries.add(entry);manager.entries.add(foreign);
    PlaybackState playing=new PlaybackState(3), paused=new PlaybackState(2);
    String line="Chinese lyric / translation\\nsecond line";
    FlymeStatusBarLyrics.update(session,line,playing);
    if(args.length>0){
      check(manager.posts==0&&entry.n==original,"unsupported device must be untouched");
      FlymeStatusBarLyrics.clear();
      check(manager.posts==0,"unsupported clear is a no-op");
      System.out.println("PASS: unsupported device");return;
    }
    int flags=Notification.FLAG_ALWAYS_SHOW_TICKER|Notification.FLAG_ONLY_UPDATE_TICKER|Notification.FLAG_NO_CLEAR;
    check(entry.n.tickerText.equals(line),"exact text including newline");
    check((entry.n.flags&flags)==flags,"all three Flyme flags");
    check((entry.n.flags&0x4000)!=0,"preserve unrelated flags");
    check(entry.n.actions==original.actions&&entry.n.icon==original.icon&&entry.n.artwork==original.artwork,"preserve media UI");
    check(entry.n.channel.equals("media"),"preserve channel");
    check(foreign.n==unrelated,"unrelated notification untouched");
    check(original.tickerText==null,"clone before mutation");
    int count=manager.posts;
    FlymeStatusBarLyrics.update(session,line,playing);
    check(manager.posts==count,"deduplicate same text");
    // Native app republishes its notification while the line remains the same.
    entry.n=original;
    FlymeStatusBarLyrics.update(session,line,playing);
    check(manager.posts==count+1,"reapply to native notification replacement");
    FlymeStatusBarLyrics.update(session,line,paused);
    check(entry.n.tickerText==null&&(entry.n.flags&flags)==0,"pause clears and restores no-clear");
    check(manager.entries.size()==2,"never cancel media notification");
    count=manager.posts;FlymeStatusBarLyrics.clear();
    check(manager.posts==count,"repeated clear is a no-op");
    original.flags|=Notification.FLAG_NO_CLEAR;entry.n=original;
    FlymeStatusBarLyrics.update(session,line,playing);FlymeStatusBarLyrics.clear();
    check((entry.n.flags&Notification.FLAG_NO_CLEAR)!=0,"preserve preexisting no-clear");
    FlymeStatusBarLyrics.update(session,line,playing);
    FlymeStatusBarLyrics.update(other,"new track",playing);
    check(entry.n.tickerText==null&&foreign.n.tickerText.equals("new track"),"session switch clears old session");
    FlymeStatusBarLyrics.update(other,"  ",playing);
    check(foreign.n.tickerText==null,"blank clears");
    FlymeStatusBarLyrics.update(session,line,playing);
    FlymeStatusBarLyrics.update(session,line,null);
    check(entry.n.tickerText==null,"null playback state clears");
    for(Settings.Setting setting:new Settings.Setting[]{Settings.LYRICS_FLYME_STATUS_BAR,Settings.LYRICS_ENABLED,Settings.LYRICS_MEDIASESSION}){
      FlymeStatusBarLyrics.update(session,line,playing);setting.value=false;
      FlymeStatusBarLyrics.update(session,line,playing);
      check(entry.n.tickerText==null,"each disabled switch clears");setting.value=true;
    }
    manager.fail=true;
    FlymeStatusBarLyrics.update(session,line,playing);FlymeStatusBarLyrics.update(session,line,playing);
    check(Logger.errors==1,"failures are isolated and rate limited");
    manager.fail=false;FlymeStatusBarLyrics.update(session,line,playing);
    check(entry.n.tickerText.equals(line),"retry after a failed notification post");
    FlymeStatusBarLyrics.clear();manager.entries.clear();
    FlymeStatusBarLyrics.update(session,line,playing);
    manager.entries.add(entry);FlymeStatusBarLyrics.update(session,line,playing);
    check(entry.n.tickerText.equals(line),"late media notification is picked up");
    Utils.context=null;FlymeStatusBarLyrics.clear();
    System.out.println("PASS: notification lifecycle, identity, text, flags, retry and cleanup");
  }
}
""",
}


def main():
    work = ROOT / "work"
    work.mkdir(exist_ok=True)
    with tempfile.TemporaryDirectory(prefix="bridge-tests-", dir=work) as temp:
        temp = Path(temp)
        for name, source in STUBS.items():
            path = temp / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(source, encoding="utf-8")
        shutil.copy2(BRIDGE, temp / "FlymeStatusBarLyrics.java")
        classes = temp / "classes"
        classes.mkdir()
        sources = [str(p) for p in temp.rglob("*.java")]
        subprocess.run(["javac", "-encoding", "UTF-8", "-d", str(classes), *sources], check=True)
        for mode in ([], ["unsupported"]):
            subprocess.run(["java", "-cp", str(classes), "BridgeTest", *mode], check=True)


if __name__ == "__main__":
    main()
