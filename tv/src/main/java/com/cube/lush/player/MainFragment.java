package com.cube.lush.player;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.ClassPresenterSelector;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;

import com.cube.lush.player.handler.ResponseHandler;
import com.cube.lush.player.manager.MediaManager;
import com.cube.lush.player.model.Channel;
import com.cube.lush.player.model.MediaContent;
import com.cube.lush.player.presenter.ChannelPresenter;
import com.cube.lush.player.presenter.MediaPresenter;
import com.cube.lush.player.util.MediaSorter;

import java.util.Arrays;
import java.util.List;

import android.support.v17.leanback.widget.InvisibleRowPresenter;
import android.support.v17.leanback.widget.PageRow;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Modified the default BrowseFragment to use PageRows, instead of ListRows.
 * <p/>
 * This is to meet the design requirements that (i) only one row is displayed at once, instead of the grid behaviour implemented by RowsFragment, and (ii) to
 * enable a vertical grid style for the home row.
 * <p>
 * Created by tim on 24/11/2016.
 */
public class MainFragment extends LushBrowseFragment
{
	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);
		initialiseMenu();
	}

	private void initialiseMenu()
	{
		// Create the objects backing the main menu
		PageRow homeRow = new PageRow(new HeaderItem("Home"));
		PageRow liveRow = new PageRow(new HeaderItem("Live"));
		PageRow channelsRow = new PageRow(new HeaderItem("Channels"));

		// Setup the fragment factory for the menu items
		MainMenuFragmentFactory fragmentFactory = new MainMenuFragmentFactory();
		fragmentFactory.registerFragment(homeRow, new HomeFragment());
		fragmentFactory.registerFragment(liveRow, new MediaDetailsFragment());
		fragmentFactory.registerFragment(channelsRow, new ChannelsFragment());
		getMainFragmentRegistry().registerFragment(PageRow.class, fragmentFactory);

		// Create and populate the main adapter
		ClassPresenterSelector mainPresenterSelector = new ClassPresenterSelector();
		mainPresenterSelector.addClassPresenter(PageRow.class, new InvisibleRowPresenter());
		ArrayObjectAdapter mainAdapter = new ArrayObjectAdapter(mainPresenterSelector);
		mainAdapter.addAll(0, Arrays.asList(homeRow, liveRow, channelsRow));
		setAdapter(mainAdapter);
	}

	private static class MainMenuFragmentFactory extends FragmentFactory<Fragment>
	{
		private Map<Object, Fragment> fragmentRegistry = new HashMap<>();

		@Override
		public Fragment createFragment(Object row)
		{
			return fragmentRegistry.get(row);
		}

		public void registerFragment(Object item, Fragment fragment)
		{
			fragmentRegistry.put(item, fragment);
		}
	}
}