package richard.rmysql;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class ConnectionPage extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.content_connection_page);

	}

	@Override
	public void onBackPressed() {
		finish();
	}

}
