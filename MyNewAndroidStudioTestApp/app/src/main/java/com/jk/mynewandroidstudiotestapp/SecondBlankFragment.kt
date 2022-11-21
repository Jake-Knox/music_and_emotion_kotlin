package com.jk.mynewandroidstudiotestapp

import android.R.attr.data
import android.app.Activity
import android.graphics.Bitmap
import android.net.Uri.parse
import android.os.Bundle
import android.provider.MediaStore
import android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.jk.mynewandroidstudiotestapp.databinding.ActivityMainBinding
import com.jk.mynewandroidstudiotestapp.databinding.FragmentSecondBlankBinding
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.android.appremote.api.error.CouldNotFindSpotifyApp
import com.spotify.android.appremote.api.error.NotLoggedInException
import com.spotify.android.appremote.api.error.UserNotAuthorizedException
import com.spotify.protocol.types.Track
import com.spotify.protocol.types.Uri


class SecondBlankFragment : Fragment() {

    lateinit var binding : FragmentSecondBlankBinding
    lateinit var viewBinding: ActivityMainBinding
    lateinit var navController: NavController

    lateinit var myActivity: Activity

    lateinit var myViewModel: MyViewModel
    lateinit var myModel: MyModel

    lateinit var emotion: String
    lateinit var currentPlaylist: String

    // Spotify
    private val clientId = "be9780c5d93143d59074bfde8289c23d"
    private val redirectUri = "http://localhost:3000/"
    private var spotifyAppRemote: SpotifyAppRemote? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        //return inflater.inflate(R.layout.fragment_second_blank, container, false)
        binding = FragmentSecondBlankBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        myViewModel = ViewModelProvider(requireActivity()).get(MyViewModel::class.java)
        var myModel = myViewModel.myLiveModel.value

        navController = findNavController()

        val emotionText = myModel?.getEmotion()
        binding.emotionText.text = emotionText

        if (emotionText != null) {
            emotion = emotionText
        }

        binding.backButton.setOnClickListener{

            onStop()
            navController.navigate(R.id.action_secondBlankFragment_to_firstBlankFragment)
        }
        }

    override fun onStart() {
        super.onStart()
        // We will start writing our code here.
        // Set the connection parameters
        val connectionParams = ConnectionParams.Builder(clientId)
            .setRedirectUri(redirectUri)
            .showAuthView( true)
            .build()

        SpotifyAppRemote.connect(requireContext(), connectionParams, object : Connector.ConnectionListener{
            override fun onConnected(appRemote: SpotifyAppRemote) {
                spotifyAppRemote = appRemote
                Log.d("MainActivity", "Connected! yay!")
                connected()
            }

            override fun onFailure(error : Throwable) {

                //Log.e("MainActivity", error.message, error)

                if(error is CouldNotFindSpotifyApp)
                {
                    Log.d("Error", "Could not find spotify app")
                }
                if (error is NotLoggedInException) {
                    Log.d("Error", "Not Logged in")

                }
                if (error is UserNotAuthorizedException) {
                    Log.d("Error", "User not autherorized")

                }
            }
        })
    }

    private fun connected() {
        // Then we will write some more code here.
        // Play a playlist
        // HAPPY, SAD, ANGRY, SURPRISED, NEUTRAL
        //
        // Feel Good Indie tunes: spotify:playlist:37i9dQZF1DX2sUQwD7tbmL
        // Sweet Piano: spotify:playlist:37i9dQZF1DX7K31D69s4M1
        // Alt 70s: spotify:playlist:37i9dQZF1DXb3ZjVksUlfu

        // Road to punk: spotify:playlist:37i9dQZF1DWU0FBqUeZYeN
        // Sad 60s: spotify:playlist:37i9dQZF1DX2m7zA91yge5
        // Oblique: spotify:playlist:37i9dQZF1DWWv8B5EWK7bn
        // Feel good Friay: spotify:playlist:37i9dQZF1DX1g0iEXLFycr
        // Britpop: spotify:playlist:37i9dQZF1DXaVgr4Tx5kRF

        var playlistURI = "spotify:playlist:37i9dQZF1DX7K31D69s4M1" // Piano
        currentPlaylist = "Piano in the Background"

            if(emotion == "HAPPY")
        {
            playlistURI = "spotify:playlist:37i9dQZF1DX1g0iEXLFycr" // Feel good Friday
            currentPlaylist = "Feel Good Friday"
        }
        else if (emotion == "SAD")
        {
            playlistURI = "spotify:playlist:37i9dQZF1DX2m7zA91yge5" // Sad 60s
            currentPlaylist = "Sad 60s"
        }
        else if (emotion == "SURPRISED")
        {
            playlistURI = "spotify:playlist:37i9dQZF1DWWv8B5EWK7bn" // Oblique
            currentPlaylist = "Oblique"
        }
        else if (emotion == "ANGRY")
        {
            playlistURI = "spotify:playlist:37i9dQZF1DWU0FBqUeZYeN" // Road to punk
            currentPlaylist = "The Road to Punk Rock"
        }
        else if (emotion == "NEUTRAL")
        {
            playlistURI = "spotify:playlist:37i9dQZF1DXaVgr4Tx5kRF" // Britpop
            currentPlaylist = "Britpop, Etc."
        }

        spotifyAppRemote?.playerApi?.play(playlistURI)

        //subsribe to playerstate
        spotifyAppRemote?.playerApi?.subscribeToPlayerState()?.setEventCallback {
            val track: Track = it.track

            val trackName = track.name
            val artistName = track.artist.name

            // If more time, include image
            //val image = (track.imageUri) // imageUri
            //val bitmap: Bitmap = MediaStore.Images.Media.getContentUri()
            //Log.d("MainActivity", track.name + " by " + track.artist.name)

            binding.playlistText.text = currentPlaylist
            binding.songText.text = trackName
            binding.artistText.text = artistName

        }

    }

    override fun onStop() {
        super.onStop()
        // Aaand we will finish off here.
        spotifyAppRemote?.playerApi?.pause()
        spotifyAppRemote?.let {
            SpotifyAppRemote.disconnect(it)
        }

    }





    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment SecondBlankFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            SecondBlankFragment().apply {
                arguments = Bundle().apply {

                }
            }
    }
}

