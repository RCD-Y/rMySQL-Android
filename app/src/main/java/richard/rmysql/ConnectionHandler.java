package richard.rmysql;

import android.content.Context;
import android.support.v7.app.AlertDialog;
import android.view.View;

import rMySQLCore.rMySQL;

public class ConnectionHandler {

	private View connPage;
	private rMySQL r;
	private Context context;

	ConnectionHandler(View connPage) {
		this.connPage=connPage;
	}

	public void setContext(Context context) {
		this.context=context;
	}

	public void setConnection(rMySQL r) {
		this.r=r;
	}

	public void selectClicked() {
		AlertDialog.Builder builder=new AlertDialog.Builder(context);
		//builder.setView
	}


}
