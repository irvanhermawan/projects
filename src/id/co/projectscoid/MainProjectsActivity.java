package id.co.projectscoid;




import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Intent;
import android.widget.ImageView;

import id.co.projectscoid.components.SearchToolbar;
import id.co.projectscoid.util.DynamicLanguage;
import id.co.projectscoid.util.DynamicNoActionBarTheme;
import id.co.projectscoid.util.DynamicTheme;


public class MainProjectsActivity extends PassphraseRequiredActionBarActivity{
    private static final String TAG = ConversationListActivity.class.getSimpleName();

    private final DynamicTheme    dynamicTheme    = new DynamicNoActionBarTheme();
    private final DynamicLanguage dynamicLanguage = new DynamicLanguage();


    @Override
    protected void onPreCreate() {
        dynamicTheme.onCreate(this);
        dynamicLanguage.onCreate(this);
    }
    @Override
    protected void onCreate(Bundle icicle, boolean ready) {
         setContentView(R.layout.activity_main_projects);
         /*  Intent intent = new Intent(this, RegistrationActivity.class);
         intent.putExtra(RegistrationActivity.RE_REGISTRATION_EXTRA, true);
         startActivity(intent); */
        // Intent intent = new Intent(this, ConversationActivity.class);
        // startActivity(intent);
    }


}
