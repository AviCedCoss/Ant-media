package com.example.antmedia

import android.content.Context
import android.view.View
import android.widget.ImageView
import androidx.core.content.ContextCompat
import io.antmedia.webrtcandroidframework.WebRTCClient
import org.webrtc.SurfaceViewRenderer

object WebRtcConstants {
    private const val SERVER_ADDRESS = "ant-media.pandoproject.org/WebRTCApp/"

    //    private const val SERVER_ADDRESS = "ant-media.pandotest.com:5443/WebRTCAppEE/"
    const val SERVER_URL = "wss://${SERVER_ADDRESS}websocket"
    const val REST_URL = "https://${SERVER_ADDRESS}rest/v2"
    const val RECONNECTION_PERIOD_MLS = 100
    const val STREAM_URL = "https://${SERVER_ADDRESS}streams/"
    const val OBS_URL = "rtmp://ant-media.pandotest.com/WebRTCAppEE/"
    var VIDEO_PATH: String = ""
    var RUNNING_STREAM_ID: Int? = null
    var IS_STREAM_RUNNING: Boolean = false

    fun onOffVideo(
        view: ImageView?,
        webRTCClient: WebRTCClient?,
        context: Context,
        cameraViewRenderer: SurfaceViewRenderer
    ) {
        if (webRTCClient?.isVideoOn == true) {
            view?.setImageDrawable(
                ContextCompat.getDrawable(
                    context,
                    R.drawable.ic_disable_video
                )
            )
            cameraViewRenderer.visibility = View.GONE
            webRTCClient.disableVideo()
        } else {
            view?.setImageDrawable(
                ContextCompat.getDrawable(
                    context,
                    R.drawable.ic_icon_awesome_video
                )
            )
            cameraViewRenderer.visibility = View.VISIBLE
            webRTCClient?.enableVideo()
        }
    }

    fun onOffAudio(view: ImageView?, webRTCClient: WebRTCClient?, context: Context) {
        if (webRTCClient?.isAudioOn == true) {
            view?.setImageDrawable(
                ContextCompat.getDrawable(
                    context,
                    R.drawable.ic_mic_off
                )
            )
            webRTCClient.disableAudio()
        } else {
            webRTCClient?.enableAudio()
            view?.setImageDrawable(
                ContextCompat.getDrawable(
                    context,
                    R.drawable.ic_awesome_microphone
                )
            )
        }
    }
}