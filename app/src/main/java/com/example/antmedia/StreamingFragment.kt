package com.example.antmedia

import android.os.*
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.example.antmedia.WebRtcConstants.RECONNECTION_PERIOD_MLS
import com.example.antmedia.WebRtcConstants.SERVER_URL
import com.example.antmedia.WebRtcConstants.onOffAudio
import com.example.antmedia.WebRtcConstants.onOffVideo
import com.example.antmedia.databinding.FragmentStreamingBinding
import de.tavendo.autobahn.WebSocket.WebSocketConnectionObserver.WebSocketCloseNotification
import io.antmedia.webrtcandroidframework.*
import io.antmedia.webrtcandroidframework.apprtc.CallActivity.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import org.webrtc.DataChannel
import org.webrtc.RendererCommon
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import kotlin.collections.ArrayList



class StreamingFragment : Fragment(), IWebRTCListener, IDataChannelObserver{

    private var isStreamNotEnded: Boolean = true
    private var isDownloaded: Boolean = false
    private lateinit var binding: FragmentStreamingBinding
    /**
     * Mode can Publish, Play or P2P
     */
    private val webRTCMode = IWebRTCClient.MODE_PUBLISH
    private val enableDataChannel = true
    private var webRTCClient: WebRTCClient? = null
    private var operationName = ""
    private var streamId: String? = null
    // variables for handling reconnection attempts after disconnected
    private var stoppedStream = false
    var reconnectionHandler = Handler(Looper.getMainLooper())
    private var reconnectionRunnable: Runnable = object : Runnable {
        override fun run() {
            if (!webRTCClient!!.isStreaming) {
                attempt2Reconnect()
                // call the handler again in case startStreaming is not successful
                reconnectionHandler.postDelayed(this, RECONNECTION_PERIOD_MLS.toLong())
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentStreamingBinding.inflate(inflater, container, false)
        setListeners()
        setupWebRtcClient()
        return binding.root
    }



    private fun setupWebRtcClient() {
        requireActivity().intent.apply {
            putExtra(EXTRA_CAPTURETOTEXTURE_ENABLED, true)
            putExtra(EXTRA_VIDEO_FPS, 20)
            putExtra(EXTRA_VIDEO_BITRATE, 1500)
            putExtra(EXTRA_AUDIO_BITRATE, 128)
            putExtra(EXTRA_CAPTURETOTEXTURE_ENABLED, true)
            putExtra(EXTRA_DATA_CHANNEL_ENABLED, enableDataChannel)
        }
        streamId = "123"
        webRTCClient = WebRTCClient(this, requireContext())
        //        webRTCClient?.onCameraSwitch()
        val tokenId = "tokenId"
        webRTCClient?.apply {
            setOpenFrontCamera(true)
            setVideoRenderers(null, binding.webView)
            init(
                SERVER_URL,
                streamId,
                webRTCMode,
                tokenId,
                requireActivity().intent
            )
            setDataChannelObserver(this)
        }
        startStreaming()
        val callback: OnBackPressedCallback =
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {

                }
            }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
    }


    private fun endVideoStream(isVideo: Boolean) {
        finishStream()
    }

    private fun finishStream() {
        isStreamNotEnded = false
        requireActivity().finish()
    }


    private fun disConnect() {
        webRTCClient?.stopStream()
    }

    private fun setListeners() {
        binding.apply {
            streamEnd.setOnClickListener {
            }

            cameraFlip.setOnClickListener {
                webRTCClient?.switchCamera()
            }

            mute.setOnClickListener {
                onOffAudio(mute, webRTCClient, requireContext())
            }
            videoEnabled.setOnClickListener {
                onOffVideo(
                    videoEnabled,
                    webRTCClient,
                    requireContext(),
                    binding.webView
                )
            }
        }
    }

    private fun startStreaming() {
        if (!webRTCClient?.isStreaming!!) {
            webRTCClient?.startStream()
        }
    }

    override fun onDestroy() {
        disConnect()
        Log.i("TAG", "onDestroy: Called")
/*        if (isStreamNotEnded)
            endVideoStream(false)*/
        super.onDestroy()
    }

    private fun attempt2Reconnect() {
        Log.w(javaClass.simpleName, "Attempt2Reconnect called")
        if (!webRTCClient?.isStreaming!!) {
            webRTCClient?.startStream()
        }
    }

    override fun onPlayStarted(streamId: String?) {
        Log.w(javaClass.simpleName, "onPlayStarted")
        Toast.makeText(requireContext(), "Play started", Toast.LENGTH_LONG).show()
        webRTCClient?.switchVideoScaling(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
        webRTCClient?.getStreamInfoList()
    }

    override fun onPublishStarted(streamId: String?) {
        Log.w(javaClass.simpleName, "onPublishStarted")

    }

    override fun onPublishFinished(streamId: String?) {
        Log.w(javaClass.simpleName, "onPublishFinished")
        Toast.makeText(requireContext(), "Publish finished", Toast.LENGTH_LONG).show()
    }

    override fun onPlayFinished(streamId: String?) {
        Log.w(javaClass.simpleName, "onPlayFinished")
        Toast.makeText(requireContext(), "Play finished", Toast.LENGTH_LONG).show()
    }

    override fun noStreamExistsToPlay(streamId: String?) {
        Log.w(javaClass.simpleName, "noStreamExistsToPlay")
        Toast.makeText(requireContext(), "No stream exist to play", Toast.LENGTH_LONG)
            .show()
        requireActivity().finish()
    }

    override fun streamIdInUse(streamId: String?) {
        Log.w(javaClass.simpleName, "streamIdInUse")
        Toast.makeText(requireContext(), "Stream id is already in use.", Toast.LENGTH_LONG)
            .show()
    }

    override fun onError(description: String, streamId: String?) {
        Toast.makeText(requireContext(), "Error: $description", Toast.LENGTH_LONG).show()
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onSignalChannelClosed(
        code: WebSocketCloseNotification,
        streamId: String?
    ) {
        Toast.makeText(
            requireContext(),
            "Signal channel closed with code $code",
            Toast.LENGTH_LONG
        )
            .show()
    }

    override fun onDisconnected(streamId: String?) {
        Log.w(javaClass.simpleName, "disconnected")
        // handle reconnection attempt
        /*if (!stoppedStream) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (!reconnectionHandler.hasCallbacks(reconnectionRunnable)) {
                    reconnectionHandler.postDelayed(
                        reconnectionRunnable,
                        RECONNECTION_PERIOD_MLS.toLong()
                    )
                }
            } else {
                reconnectionHandler.postDelayed(
                    reconnectionRunnable,
                    RECONNECTION_PERIOD_MLS.toLong()
                )
            }
        } else {
            Toast.makeText(requireContext(), "Stopped the stream", Toast.LENGTH_LONG).show()
            stoppedStream = false
        }*/
    }

    override fun onIceConnected(streamId: String?) {
        //it is called when connected to ice
        // remove scheduled reconnection attempts
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (reconnectionHandler.hasCallbacks(reconnectionRunnable)) {
                reconnectionHandler.removeCallbacks(reconnectionRunnable, null)
            }
        } else {
            reconnectionHandler.removeCallbacks(reconnectionRunnable, null)
        }
    }

    override fun onIceDisconnected(streamId: String?) {
        //it's called when ice is disconnected
    }

    override fun onTrackList(tracks: Array<String?>?) {}

    override fun onBitrateMeasurement(
        streamId: String,
        targetBitrate: Int,
        videoBitrate: Int,
        audioBitrate: Int
    ) {
        Log.e(
            javaClass.simpleName,
            "st:$streamId tb:$targetBitrate vb:$videoBitrate ab:$audioBitrate"
        )
        if (targetBitrate < videoBitrate + audioBitrate) {
            Toast.makeText(requireContext(), "low bandwidth", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onStreamInfoList(
        streamId: String?,
        streamInfoList: ArrayList<StreamInfo>
    ) {

    }

    override fun onBufferedAmountChange(
        previousAmount: Long,
        dataChannelLabel: String?
    ) {
        Log.d(
            StreamingFragment::class.java.name,
            "Data channel buffered amount changed: "
        )
    }

    override fun onStateChange(
        state: DataChannel.State?,
        dataChannelLabel: String?
    ) {
        Log.d(
            StreamingFragment::class.java.name,
            "Data channel state changed: "
        )
    }

    override fun onMessage(buffer: DataChannel.Buffer, dataChannelLabel: String?) {
        val data = buffer.data
        val messageText = String(data.array(), StandardCharsets.UTF_8)
        Toast.makeText(requireContext(), "New Message: $messageText", Toast.LENGTH_LONG)
            .show()
    }

    override fun onMessageSent(buffer: DataChannel.Buffer, successful: Boolean) {
        if (successful) {
            val data = buffer.data
            val bytes = ByteArray(data.capacity())
            data[bytes]
            val messageText = String(bytes, StandardCharsets.UTF_8)
            Toast.makeText(requireContext(), "Message is sent", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(
                requireContext(),
                "Could not send the text message",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun sendTextMessage(messageToSend: String) {
        val buffer = ByteBuffer.wrap(messageToSend.toByteArray(StandardCharsets.UTF_8))
        val buf = DataChannel.Buffer(buffer, false)
        webRTCClient?.sendMessageViaDataChannel(buf)
    }

    /*  override fun onProgressUpdate(percentage: Int) {
          dialog.updateProgress(percentage)
      }

      override fun onError() {


      }

      override fun onFinish() {
          dialog.updateProgress(100)
      }*/

    private fun getFile() {
        CoroutineScope(IO).launch {
            if (isDownloaded)
                return@launch
            isDownloaded = true
            withContext(Main) {
                endVideoStream(true)
            }
        }
    }


}