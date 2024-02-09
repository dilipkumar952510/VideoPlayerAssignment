package com.talkcharge.hlsvideoview

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import com.talkcharge.hlsvideoview.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding:ActivityMainBinding
    private var list = ArrayList<String>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this,R.layout.activity_main)
        setData()
    }

    private fun setData() {
        list.clear()
        list.add("http://sample.vodobox.net/skate_phantom_flex_4k/skate_phantom_flex_4k.m3u8")
        list.add("http://playertest.longtailvideo.com/adaptive/wowzaid3/playlist.m3u8")
        list.add("http://cdn-fms.rbs.com.br/vod/hls_sample1_manifest.m3u8")
        list.add("http://content.jwplatform.com/manifests/vM7nH0Kl.m3u8")
        list.add("http://walterebert.com/playground/video/hls/sintel-trailer.m3u8")
        list.add("http://qthttp.apple.com.edgesuite.net/1010qwoeiuryfg/sl.m3u8")
        list.add("https://devimages.apple.com.edgekey.net/streaming/examples/bipbop_16x9/bipbop_16x9_variant.m3u8")
        list.add("https://bitdash-a.akamaihd.net/content/sintel/hls/playlist.m3u8")
        list.add("https://d1gnaphp93fop2.cloudfront.net/videos/multiresolution/rendition_new10.m3u8")

        binding.recyclerView.adapter = ListAdapter(list,this@MainActivity)
    }
}