import org.apache.cordova.CordovaActivity;

public class UniversalLinksActivity extends CordovaActivity{
  @Override
    public void onNewIntent(Intent intent) {
        handleIntent(intent);
    }
}

private void handleIntent(intent){
  // read intent
  String action = intent.getAction();Log.d("Activity action: " + action);
  Uri launchUri = intent.getData();
}
