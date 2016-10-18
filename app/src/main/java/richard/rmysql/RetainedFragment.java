package richard.rmysql;

import android.app.Fragment;
import android.os.Bundle;
import android.view.View;

import java.util.ArrayList;

public class RetainedFragment extends Fragment {
	private ArrayList<ConnectionHandler> connHandlers;
	private ArrayList<View> subViews;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
	}

	public void setConnHandlers(ArrayList<ConnectionHandler> connHandlers) {
		this.connHandlers=connHandlers;
	}

	public void setSubViews(ArrayList<View> subViews) {
		this.subViews=subViews;
	}

	public ArrayList<ConnectionHandler> getConnHandlers() {
		return connHandlers;
	}

	public ArrayList<View> getSubViews() {
		return subViews;
	}
}
