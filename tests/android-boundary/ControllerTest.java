package io.github.kljjh.flymelyrics;
import android.app.*; import android.content.Context; import android.media.*; import android.media.session.*; import android.os.Handler; import android.service.notification.*;
import app.morphe.extension.music.patches.lyrics.LyricsManager; import app.morphe.extension.music.settings.Settings;
public class ControllerTest {
 public static void main(String[] args){
    Context c=new Context(); MediaSession session=new MediaSession(); Notification n=new Notification();
    n.extras.put(Notification.EXTRA_MEDIA_SESSION,session.getSessionToken()); StatusBarNotification e=new StatusBarNotification(null,5,n);c.manager.entries.add(e);
    FlymeController.onMetadata(c,session,new MediaMetadata(" Song ","Artist")); FlymeController.onPlaybackState(c,new PlaybackState(3));
    Handler.tick(); assert "line\ntranslation".equals(e.n.tickerText); // no mini-player setting or view exists in this fixture
    FlymeController.setEnabled(c,false); assert Handler.pending==null && e.n.tickerText==null;
    FlymeController.onPlaybackState(c,new PlaybackState(3)); assert Handler.pending==null;
    FlymeController.setEnabled(c,true); LyricsManager.getInstance().line=null; Handler.tick(); assert " Song ".equals(e.n.tickerText);
    LyricsManager.getInstance().synced=false; Handler.tick(); assert e.n.tickerText==null;
    LyricsManager.getInstance().synced=true; FlymeController.onMetadata(c,session,new MediaMetadata("Different","Artist"));Handler.tick();assert e.n.tickerText==null;
    FlymeController.onMetadata(c,session,new MediaMetadata("Song","Artist")); Handler.tick(); assert "Song".equals(e.n.tickerText);
    Settings.LYRICS_ENABLED.value=false; FlymeController.refresh(); assert Handler.pending==null && e.n.tickerText==null;
    Settings.LYRICS_ENABLED.value=true; FlymeController.refresh(); Handler.tick();assert "Song".equals(e.n.tickerText);
    LyricsManager m=LyricsManager.getInstance(); m.timeline=true;
    Handler.tick(); assert "next".equals(e.n.tickerText); // default -800: 1000ms reads 1800ms
    assert "previous".equals(m.getCurrentLineText()); // official remains +400
    FlymeTiming.save(c, 0); Handler.tick(); assert "previous".equals(e.n.tickerText);
    m.playback=1800; Handler.tick(); assert "next".equals(e.n.tickerText);
    FlymeTiming.save(c, 500); Handler.tick(); assert "previous".equals(e.n.tickerText);
    m.playback=2000; Handler.tick(); assert "next".equals(e.n.tickerText);
    FlymeTiming.save(c, FlymeTiming.FOLLOW); m.official=600; Handler.tick(); assert "previous".equals(e.n.tickerText);
    m.official=-800; Handler.tick(); assert "next".equals(e.n.tickerText);
    m.fail=true; Handler.tick(); assert !FlymeTiming.isStatusRead() && FlymeTiming.offset(123)==123; m.fail=false;
    FlymeController.onPlaybackState(c,new PlaybackState(2)); assert Handler.pending==null && e.n.tickerText==null;
    System.out.println("PASS: independent default, persisted off, immediate clearing, matching, title fallback and master switch");
 }
}
