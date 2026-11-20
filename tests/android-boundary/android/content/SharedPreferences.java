package android.content;
public class SharedPreferences { private Boolean saved; public boolean getBoolean(String k,boolean def){return saved==null?def:saved;} public SharedPreferences edit(){return this;} public SharedPreferences putBoolean(String k,boolean v){saved=v;return this;} public void apply(){} }
