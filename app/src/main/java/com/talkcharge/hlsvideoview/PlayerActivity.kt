package com.talkcharge.hlsvideoview

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.MappingTrackSelector
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.exoplayer2.util.Util
import com.talkcharge.hlsvideoview.databinding.ActivityPlayerBinding


class PlayerActivity : AppCompatActivity() {
    private lateinit var binding : ActivityPlayerBinding
    private var qualityList = mutableListOf<String>()
    private var qualityForShow = mutableListOf<String>()
    private lateinit var exoPlayer: SimpleExoPlayer
    private var playbackPosition: Long = 0
    private val handler = Handler()
    private var isPlaying = true
    private var isControllerShowing = true
    private val runnable = object : Runnable {
        override fun run() {
            val currentPosition = exoPlayer.currentPosition
            val duration = exoPlayer.duration
            val progress = ((currentPosition * 1000) / duration).toInt()
            binding.seekbar.progress = progress
            binding.min.text = formatDuration(currentPosition)
            handler.postDelayed(this, 1000) // Update every second (1000 ms)
        }
    }

    private fun formatDuration(durationMs: Long): String {
        val totalSeconds = durationMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
        binding = DataBindingUtil.setContentView(this,R.layout.activity_player)
        binding.click = CLickAction()
        setVideoPlayer()
        setListener()
    }

    private fun setListener() {
        exoPlayer.addListener(object : Player.EventListener {               // used for get video resolution when video is ready
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    getVideoQuality()
                    handler.post(runnable)
                    binding.max.text = formatDuration(exoPlayer.duration)
                }else
                    handler.removeCallbacks(runnable)
            }

            override fun onPlayerError(error: PlaybackException) {
                super.onPlayerError(error)
                Toast.makeText(this@PlayerActivity, error.localizedMessage, Toast.LENGTH_SHORT).show()
            }
        })
        binding.seekbar.setOnSeekBarChangeListener(object :SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                if (p2) {
                    val progress = p1 * exoPlayer.duration / 1000
//                    binding.seekbar.progress = progress.toInt()
                    binding.min.text = formatDuration(progress)
                    exoPlayer.seekTo(progress)
                }
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {}

            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })
        binding.resolutionSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                if (p2!=0){
                    changeVideoQuality(qualityList[p2-1])
                }else {
                    val trackSelector = exoPlayer.trackSelector as DefaultTrackSelector
                    trackSelector.parameters = DefaultTrackSelector(this@PlayerActivity).parameters
                    exoPlayer.playWhenReady = true
                }
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }
    }


    private fun setVideoPlayer() {
        exoPlayer = SimpleExoPlayer.Builder(this)           // initializing simple exoplayer with default track means auto resolution
            .setTrackSelector(DefaultTrackSelector(this))
            .setLoadControl(DefaultLoadControl())
            .build()

        playVideo()
        setVideoQualityAccordingNetwork()
    }

    private fun getAvailableResolutions(): List<String> {  // it will return the all resolution of video which are available in HLS url
        val resolutions = mutableListOf<String>()

        for (groupIndex in 0 until exoPlayer.currentTrackGroups.length) {
            val trackGroup = exoPlayer.currentTrackGroups[groupIndex]
            for (trackIndex in 0 until trackGroup.length) {
                val format = trackGroup.getFormat(trackIndex)
                if (MimeTypes.isVideo(format.sampleMimeType)) {
                    val resolution = "${format.width}x${format.height}"
                    resolutions.add(resolution)
                }
            }
        }
        return resolutions.distinct()
    }

    private fun playVideo(){
        binding.exoplayer.player = exoPlayer    // player-view will initialize with simple exoplayer

        // now preparing the new media source with url
        val dataSourceFactory = DefaultDataSourceFactory(this, Util.getUserAgent(this, getString(R.string.app_name)))
        val hlsMediaSource = HlsMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(Uri.parse(intent.getStringExtra("url"))))

        exoPlayer.prepare(hlsMediaSource)

        exoPlayer.seekTo(playbackPosition)  // Restore the playback position
        exoPlayer.playWhenReady = true

        binding.seekbar.max = 1000
    }

    private fun changeVideoQuality(resolution: String) {        //it will change the video quality according to our choice
        playbackPosition = exoPlayer.currentPosition

        val trackSelector = exoPlayer.trackSelector as DefaultTrackSelector
        var parameters = trackSelector.parameters

        val videoRendererIndex = 0
        val trackGroups = (exoPlayer.trackSelector as MappingTrackSelector).currentMappedTrackInfo!!.getTrackGroups(videoRendererIndex)

        for (groupIndex in 0 until trackGroups.length) {
            val trackGroup = trackGroups.get(groupIndex)
            for (trackIndex in 0 until trackGroup.length) {
                val format = trackGroup.getFormat(trackIndex)
                if (MimeTypes.isVideo(format.sampleMimeType) && "${format.width}x${format.height}" == resolution) {
                    val trackSelection =
                        DefaultTrackSelector.SelectionOverride(groupIndex, trackIndex)
                    parameters =
                        parameters.buildUpon().setSelectionOverride(videoRendererIndex, trackGroups, trackSelection)
                            .build()
                }
            }
        }

        // Release the current player instance and create a new one
        exoPlayer.release()
        exoPlayer = SimpleExoPlayer.Builder(this)       // again simple player will initialize with new resolution
            .setTrackSelector(trackSelector)
            .setLoadControl(DefaultLoadControl())
            .build()

        playVideo()     // again calling hls url to play
    }

    private fun getVideoQuality() {
        qualityList = getAvailableResolutions().toMutableList()  // it will return the all resolution of video which are available in HLS url
        qualityList.sortByDescending { resolution ->
            val (width, height) = resolution.split("x").map { it.toInt() }
            width * height
        }
        qualityForShow.clear()
        qualityForShow.add("Auto")
        for(i in qualityList.indices){
            qualityForShow.add(getHeightFromResolution(qualityList[i])+"p")
        }
        binding.resolutionSpinner.adapter = ArrayAdapter(this, R.layout.spinner_item, qualityForShow)
    }

    private fun getHeightFromResolution(resolution: String): String {
        val dimensions = resolution.split("x")
        if (dimensions.size == 2) {
            val width = dimensions[0].toIntOrNull()
            val height = dimensions[1].toIntOrNull()

            if (width != null && height != null) {
                return height.toString()
            }
        }
        return ""
    }

    private fun setVideoQualityAccordingNetwork() {  // this will set your player quality according to network type
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCapabilities =
            connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)

        if (networkCapabilities != null) {
            if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                // in this situation video quality will be high
                exoPlayer.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
            } else if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                // in this situation video quality will be low
                exoPlayer.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
            }
        }
    }

    override fun onPause() {
        super.onPause()
        exoPlayer.pause()
    }

    override fun onStop() {
        super.onStop()
        exoPlayer.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer.release()
    }

    inner class CLickAction{
        fun onBackward(view: View){
            handler.removeCallbacks(runnable)
            exoPlayer.seekTo(exoPlayer.currentPosition-10000)
        }

        fun onForward(view: View){
            handler.removeCallbacks(runnable)
            exoPlayer.seekTo(exoPlayer.currentPosition+10000)
        }

        fun onPlayPause(view: View){
            if (isPlaying) {
                binding.play.setImageResource(R.drawable.play)
                isPlaying = false
                exoPlayer.pause()
            }else{
                binding.play.setImageResource(R.drawable.pause)
                isPlaying = true
                exoPlayer.playWhenReady = true
            }
        }

        fun onMainClick(view: View){
            if (isControllerShowing){
                binding.seekLayout.isVisible = false
                binding.controller.isVisible = false
                isControllerShowing = false
            }else{
                binding.seekLayout.isVisible = true
                binding.controller.isVisible = true
                isControllerShowing = true
            }
        }
    }
}