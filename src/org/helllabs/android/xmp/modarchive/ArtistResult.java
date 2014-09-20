package org.helllabs.android.xmp.modarchive;

import java.util.List;

import org.helllabs.android.xmp.R;
import org.helllabs.android.xmp.modarchive.model.Artist;
import org.helllabs.android.xmp.modarchive.request.ArtistRequest;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class ArtistResult extends Result implements ArtistRequest.OnResponseListener<List<Artist>>, ListView.OnItemClickListener {
	
	public static final String ARTIST_ID = "artist_id";
	private Context context;
	private ListView list;
	private List<Artist> artistList;

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.result_list);
		
		context = this;
		list = (ListView)findViewById(R.id.result_list);
		
		final String search = getIntent().getStringExtra(Search.SEARCH);
		
		final String key = getString(R.string.modarchive_apikey);
		final ArtistRequest request = new ArtistRequest(key, "search_artist&query=" + search);
		request.setOnResponseListener(this).send();
		
		list.setOnItemClickListener(this);
	}
	
	@Override
	public void onResponse(final List<Artist> response) {
		artistList = response;
		final ArrayAdapter<Artist> adapter = new ArrayAdapter<Artist>(context, android.R.layout.simple_list_item_1, response);
		list.setAdapter(adapter);
	}

	@Override
	public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
		final Intent intent = new Intent(this, ArtistModulesResult.class);
		intent.putExtra(ARTIST_ID, artistList.get(position).getId());
		startActivity(intent);
	}



}