package richard.rmysql;

import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.util.TypedValue;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.jcraft.jsch.JSchException;

import java.sql.SQLException;
import java.util.ArrayList;

import rMySQLCore.rMySQL;

public class MainActivity extends AppCompatActivity
		implements NavigationView.OnNavigationItemSelectedListener {

	private ArrayList<View> subViews=new ArrayList<>();
	private ArrayList<ConnectionHandler> connHandlers=new ArrayList<>();
	private NavigationView navigationView;
	private AlertDialog dialogView=null;
	private AlertDialog connectingDialogView=null;
	private rMySQL tempConnection=null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
				this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
		drawer.setDrawerListener(toggle);
		toggle.syncState();

		navigationView = (NavigationView) findViewById(R.id.nav_view);
		navigationView.setNavigationItemSelectedListener(this);
	}

	@Override
	public void onBackPressed() {
		DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		if (drawer.isDrawerOpen(GravityCompat.START)) {
			drawer.closeDrawer(GravityCompat.START);
		} else {
			super.onBackPressed();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();

		if (id == R.id.action_close_connection) {
		}

		return super.onOptionsItemSelected(item);
	}

	@SuppressWarnings("StatementWithEmptyBody")
	@Override
	public boolean onNavigationItemSelected(MenuItem item) {
		// Handle navigation view item clicks here.
		int id = item.getItemId();
		if (id == R.id.nav_home) {
			changeDisplayView(R.layout.content_home);
		} else if (id == R.id.nav_settings) {
			Snackbar.make(findViewById(R.id.app_bar),"Settings unavailable",1000).show();
		} else if (id == R.id.nav_about) {
			Snackbar.make(findViewById(R.id.app_bar),"About unavailable",1000).show();
		} else {
			changeDisplayView(subViews.get(item.getOrder()-1));
		}

		DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		drawer.closeDrawer(GravityCompat.START);
		return true;
	}

	public View changeDisplayView(int layoutResource) {
		LinearLayout viewParent=(LinearLayout) findViewById(R.id.view_parent);
		viewParent.removeAllViewsInLayout();
		ViewStub viewStub=new ViewStub(this);
		viewStub.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		viewStub.setLayoutResource(layoutResource);
		viewParent.addView(viewStub);
		View view=viewStub.inflate();
		Animation animation = AnimationUtils.makeInAnimation(this, true);
		animation.setDuration(300);
		view.startAnimation(animation);
		return view;
	}

	public void changeDisplayView(View view) {
		LinearLayout viewParent=(LinearLayout) findViewById(R.id.view_parent);
		viewParent.removeAllViewsInLayout();
		viewParent.addView(view);
		Animation animation = AnimationUtils.makeInAnimation(this, true);
		animation.setDuration(300);
		view.startAnimation(animation);
	}

	public void createClick(View v) {
		AlertDialog.Builder builder=new AlertDialog.Builder(this);
		builder.setTitle(R.string.dialog_create_title);
		builder.setView(R.layout.dialog_create_connection);
		builder.setPositiveButton(R.string.dialog_create_connect, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				ConnTask connTask=new ConnTask();
				//execution hidden here
				//ServerAddress, Username, Password, LocalPort, RemoteAddress, RemotePort, Schema, SQLUsername, SQLPassword, SQLServerAddress
				AlertDialog.Builder builder=new AlertDialog.Builder(findViewById(R.id.app_bar).getContext());
				builder.setCancelable(false);
				builder.setTitle("Connecting...");
				ProgressBar loadingAnimation=new ProgressBar(findViewById(R.id.app_bar).getContext());
				loadingAnimation.setPadding(0,getPixels(16),0,0);
				builder.setView(loadingAnimation);
				builder.setNegativeButton("Stop", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						connectingDialogView.dismiss();
						Snackbar.make(findViewById(R.id.app_bar), R.string.dialog_connecting_aborted, 1000).show();
					}
				});
				connectingDialogView=builder.show();
				//connHandlers.get(connHandlers.size()-1).setDesc(String.valueOf(subViews.size()-1));
			}
		});
		builder.setNegativeButton(R.string.dialog_create_cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {}
		});
		dialogView=builder.show();
	}

	public void loadClick(View v) {
		Snackbar.make(findViewById(R.id.app_bar),"Presets unavailable",1000).show();
	}

	public void settingsClick(View v) {
		Snackbar.make(findViewById(R.id.app_bar),"Settings unavailable",1000).show();
	}

	public void gitHubClick(View v) {
		Snackbar.make(findViewById(R.id.app_bar),"GitHub unavailable",1000).show();
	}

	public class ConnTask extends AsyncTask<String, Void, String> {
		@Override
		protected String doInBackground(String... strings) {
			rMySQL r=new rMySQL();
			if (strings[0].equals("true")) {
				try {
					r.dellocalbound(Integer.parseInt(strings[4]));
				} catch (Exception e) {}
				try {
					r.ssh(strings[1].split(":")[0],strings[2],strings[3],Integer.parseInt(strings[1].split(":")[1]));
					r.createbound(Integer.parseInt(strings[4]),strings[5],Integer.parseInt(strings[6]));
					r.ConstructConnection("127.0.0.1:"+strings[4],strings[7],strings[8],strings[9]);
					tempConnection=r;
				} catch (JSchException | IllegalAccessException | InstantiationException | SQLException | ClassNotFoundException e) {
					e.printStackTrace();
					return e.getLocalizedMessage();
				}
			} else {
				try {
 					r.ConstructConnection(strings[10],strings[7],strings[8],strings[9]);
					tempConnection=r;
				} catch (IllegalAccessException | InstantiationException | SQLException | ClassNotFoundException e) {
					e.printStackTrace();
					return e.getLocalizedMessage();
				}
			}
			return "Success";
		}

		@Override
		protected void onPostExecute(String result) {
			if (result.equals("Success")) {
				connectingDialogView.dismiss();
				subViews.add(changeDisplayView(R.layout.content_connection_page));
				navigationView.getMenu().add(R.id.nav_group,0,subViews.size(),getString(R.string.nav_connection_untitled)+subViews.size()).setIcon(R.drawable.ic_nav_connection).setCheckable(true).setChecked(true);
				ConnectionHandler newHandler=new ConnectionHandler(subViews.get(subViews.size()-1));
				connHandlers.add(newHandler);
				newHandler.setContext(findViewById(R.id.app_bar).getContext());
				newHandler.setConnection(tempConnection);
			} else {
				connectingDialogView.dismiss();
				Snackbar.make(findViewById(R.id.app_bar),getString(R.string.dialog_connecting_failed)+": "+result,5000).show();
			}
		}
	}

	public int getPixels(int dp) {
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
	}

}
