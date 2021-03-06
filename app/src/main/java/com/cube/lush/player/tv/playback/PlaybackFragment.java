package com.cube.lush.player.tv.playback;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.brightcove.player.analytics.Analytics;
import com.brightcove.player.edge.PlaylistListener;
import com.brightcove.player.edge.VideoListener;
import com.brightcove.player.media.DeliveryType;
import com.brightcove.player.model.Playlist;
import com.brightcove.player.model.Video;
import com.brightcove.player.view.BaseVideoView;
import com.brightcove.player.view.BrightcovePlayerFragment;
import com.cube.lush.player.R;
import com.cube.lush.player.content.brightcove.BrightcoveCatalog;
import com.cube.lush.player.content.brightcove.BrightcoveUtils;
import com.cube.lush.player.content.handler.ResponseHandler;
import com.cube.lush.player.content.model.VideoInfo;
import com.cube.lush.player.content.repository.LivePlaylistRepository;
import com.lush.player.api.model.LivePlaylist;
import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.Picasso;

import java.util.List;

/**
 * Uses the Brightcove SDK player to playback Lush-related content, including playlists, specific videos, or remote files.
 *
 * @author Jamie Cruwys
 */
public class PlaybackFragment extends BrightcovePlayerFragment
{
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View view = inflater.inflate(R.layout.fragment_playback, container, false);
		brightcoveVideoView = (BaseVideoView) view.findViewById(R.id.brightcove_video_view);

		super.onCreateView(inflater, container, savedInstanceState);

		Analytics analytics = brightcoveVideoView.getAnalytics();
		analytics.setAccount(com.cube.lush.player.content.BuildConfig.BRIGHTCOVE_ACCOUNT_ID);

		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);

		Intent intent = getActivity().getIntent();

		if (intent != null)
		{
			PlaybackMethod playbackMethod = (PlaybackMethod)intent.getSerializableExtra(PlaybackActivity.EXTRA_PLAYBACK_METHOD);
			String playbackMethodValue = intent.getStringExtra(PlaybackActivity.EXTRA_PLAYBACK_METHOD_VALUE);
			String backgroundUrl = intent.getStringExtra(PlaybackActivity.EXTRA_PLAYBACK_BACKGROUND);

			if (!TextUtils.isEmpty(backgroundUrl) && brightcoveVideoView != null && brightcoveVideoView.getStillView() != null && playbackMethod != null && playbackMethod == PlaybackMethod.FILE_URL)
			{
				Picasso.with(brightcoveVideoView.getContext())
					.load(backgroundUrl)
					.memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE)
					.into(brightcoveVideoView.getStillView());
			}

			if (playbackMethod != null && !TextUtils.isEmpty(playbackMethodValue))
			{
				if (playbackMethod == PlaybackMethod.PLAYLIST)
				{
					playPlaylist(playbackMethodValue);
				}
				else if (playbackMethod == PlaybackMethod.VIDEO)
				{
					playVideo(playbackMethodValue);
				}
				else if (playbackMethod == PlaybackMethod.FILE_URL)
				{
					playFile(playbackMethodValue);
				}
			}
		}
	}

	/**
	 * Start playing the live Lush content
	 */
	public void playLive()
	{
		brightcoveVideoView.stopPlayback();

		LivePlaylistRepository.getInstance(getActivity()).getItems(new ResponseHandler<LivePlaylist>()
		{
			@Override public void onSuccess(@NonNull List<LivePlaylist> items)
			{
				// This method is designed to be called from async methods so make sure we've not lost context since then
				if (getActivity() == null)
				{
					return;
				}

				if (!items.isEmpty() && !TextUtils.isEmpty(items.get(0).getId()))
				{
					playPlaylist(items.get(0).getId());
				}
				else
				{
					getActivity().finish();
				}
			}

			@Override public void onFailure(@Nullable Throwable t)
			{
				Activity activity = getActivity();

				if (activity != null)
				{
					activity.finish();
					Toast.makeText(activity, R.string.error_live_playlist, Toast.LENGTH_LONG).show();
				}
			}
		});
	}

	/**
	 * Queue a particular Brightcove playlist in the Brightcove player.
	 *
	 * @param playlistId
	 */
	public void playPlaylist(String playlistId)
	{
		brightcoveVideoView.stopPlayback();

		if (TextUtils.isEmpty(playlistId))
		{
			return;
		}

		BrightcoveCatalog.INSTANCE.getCatalog().findPlaylistByID(playlistId, new PlaylistListener()
		{
			@Override
			public void onPlaylist(Playlist playlist)
			{
				// This method is designed to be called from async methods so make sure we've not lost context since then
				if (getActivity() == null)
				{
					return;
				}


				VideoInfo videoInfo = BrightcoveUtils.findCurrentLiveVideo(playlist);

				if (videoInfo != null)
				{
					brightcoveVideoView.add(videoInfo.getVideo());
					brightcoveVideoView.seekTo((int) (System.currentTimeMillis() - videoInfo.getStartTimeUtc()));
					brightcoveVideoView.start();
					brightcoveVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener()
					{
						@Override
						public void onCompletion(MediaPlayer mediaPlayer)
						{
							playLive();
						}
					});
				}
				else
				{
					getActivity().finish();
				}
			}

			@Override
			public void onError(String error)
			{
				super.onError(error);

				Activity activity = getActivity();

				if (activity != null)
				{
					activity.finish();
					Toast.makeText(activity, getString(R.string.error_with_details, error), Toast.LENGTH_LONG).show();
				}
			}
		});
	}

	/**
	 * Queue a particular Brightcove video in the Brightcove player.
	 *
	 * @param videoId
	 */
	public void playVideo(String videoId)
	{
		brightcoveVideoView.stopPlayback();

		if (TextUtils.isEmpty(videoId))
		{
			return;
		}

		BrightcoveCatalog.INSTANCE.getCatalog().findVideoByID(videoId, new VideoListener()
		{
			// Add the video found to the queue with add().
			// Start playback of the video with start().
			@Override
			public void onVideo(Video video)
			{
				// This method is designed to be called from async methods so make sure we've not lost context since then
				if (getActivity() == null)
				{
					return;
				}

				brightcoveVideoView.add(video);
				brightcoveVideoView.start();
				brightcoveVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener()
				{
					@Override
					public void onCompletion(MediaPlayer mediaPlayer)
					{
						Activity activity = getActivity();

						if (activity != null)
						{
							activity.finish();
						}
					}
				});
			}

			@Override public void onError(String error)
			{
				super.onError(error);

				Activity activity = getActivity();

				if (activity != null)
				{
					activity.finish();
					Toast.makeText(activity, getString(R.string.error_with_details, error), Toast.LENGTH_LONG).show();
				}
			}
		});
	}

	public void playFile(@NonNull String fileUrl)
	{
		if (brightcoveVideoView.isPlaying())
		{
			brightcoveVideoView.stopPlayback();
		}

		brightcoveVideoView.add(Video.createVideo(fileUrl, DeliveryType.MP4));
		brightcoveVideoView.start();
		brightcoveVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener()
		{
			@Override
			public void onCompletion(MediaPlayer mediaPlayer)
			{
				Activity activity = getActivity();

				if (activity != null)
				{
					activity.finish();
				}
			}
		});
	}
}
