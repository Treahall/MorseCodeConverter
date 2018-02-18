package com.example.trever.morsecodecoverter

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Bundle
import android.preference.PreferenceManager.getDefaultSharedPreferences
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import org.json.JSONObject
import java.lang.Math.round
import java.util.*
import kotlin.concurrent.timerTask

class MainActivity : AppCompatActivity() {

    private var prefs: SharedPreferences? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getDefaultSharedPreferences(this.applicationContext)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        mTextView.movementMethod = ScrollingMovementMethod()

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }

        testButton.setOnClickListener { _ ->
            appendTextAndScroll(inputText.text.toString())
            hideKeyboard()
        }

        showCodesButton.setOnClickListener {
            showCodes()
            hideKeyboard()
        }

        translateButton.setOnClickListener {
            appendTextAndScroll("\nTranslation: ")
            appendTextAndScroll(inputText.text.toString())
            translationMainDriver(inputText.text.toString())
            hideKeyboard()
        }

        Play.setOnClickListener {
            playString(" / --- ...")

            hideKeyboard()
        }

        buildDictsWithJSON(jsonObj = loadMorseJSON())
    }
    //*** end of onCreate ***

    ////////////////////Functions//////////////////////

    // Builds dictionaries from jsonObj built by loadMorseJSON()

    private var letToCodeDict: HashMap<String, String> = HashMap()
    private var codeToLetDict: HashMap<String, String> = HashMap()

    private fun buildDictsWithJSON(jsonObj : JSONObject){

        for(k in jsonObj.keys()){
            val code = jsonObj.getString(k)

            letToCodeDict.put(k, code)
            codeToLetDict.put(code, k)
        }
    }

    //Makes JSONSbject from the morse.json file in the assets folder

    private fun loadMorseJSON() : JSONObject {
        val filePath = "morse.json"

        val jsonstr = application.assets.open(filePath).bufferedReader().use{
            it.readText()
        }

        return JSONObject(jsonstr.substring(jsonstr.indexOf("{"), jsonstr.lastIndexOf("}") + 1))
    }

    private fun translationMainDriver(text: String) {
        if(isMorseCode(text))
            convertMorseToLet(text)
        else
            convertLetToMorse(text)

    }

    private fun isMorseCode(text : String) : Boolean {
        for (c in text) {
            return !(c != ' ' && c != '-' && c != '.' && c != '/')
        }
        return true
    }

    private fun convertLetToMorse(text: String) {
        var convertedText = ""
        val lowerText = text.toLowerCase()

        for ( c in lowerText) {
            convertedText += when {
                c == ' ' -> "/ "
                letToCodeDict.containsKey(c.toString()) -> letToCodeDict[c.toString()] + " "
                else -> "?"
            }
        }
        appendTextAndScroll(convertedText)
    }

    private fun convertMorseToLet(text: String) {
        var convertedText = ""
        val lowerText = text.toLowerCase()
        val textArray = lowerText.split(" ")

        for( c in textArray){
            convertedText += when {
                c == "/" -> " "
                codeToLetDict.containsKey(c) -> codeToLetDict[c]
                else -> "?"
            }
        }
        appendTextAndScroll(convertedText)
    }

    //functions to make a beep

    private val dotLength:Int = 50
    private val dashLength:Int = dotLength * 3
    //val morsePitch = prefs!!.getString("morse_pitch", "550").toInt()

    private val dotSoundBuffer: ShortArray = genSineWaveSoundBuffer(550.0, dotLength)
    private val dashSoundBuffer: ShortArray = genSineWaveSoundBuffer(550.0, dashLength)

    private val sampleRate= 44100

    private fun playString(s: String, i: Int = 0){
        if(i > s.length - 1)
            return

        //var mDelay: Long = 0

        val thenFun: () -> Unit = {
            this@MainActivity.runOnUiThread({
                playString(s, i+1)
            })
        }

        val c = s[i]
        Log.d("Log", "Processing pos: $i Char: [$c]")
        when (c) {
            '.' -> playDot(thenFun)
            '-' -> playDash(thenFun)
            '/' -> pause(6 * dotLength, thenFun)
            ' ' -> pause(2 * dotLength, thenFun)
        }
        return
    }

    private fun playDash(onDone : () -> Unit = {}) {
        Log.d("DEBUG", "playDash")
        playSoundBuffer(dashSoundBuffer, { pause(dotLength, onDone)})
    }

    private fun playDot(onDone : () -> Unit = {}) {
        Log.d("DEBUG", "playDot")
        playSoundBuffer(dotSoundBuffer, { pause(dotLength, onDone)})
    }

    private fun pause(durationMSec: Int, onDone : () -> Unit = {/* noop */}) {
        Log.d("DEBUG", "pause: " + durationMSec)
        Timer().schedule( timerTask {
            onDone()
        }, durationMSec.toLong())
    }

    private fun genSineWaveSoundBuffer(frequency: Double, durationMSec: Int) : ShortArray {

        val duration: Int = round((durationMSec / 1000.0) * sampleRate).toInt()

        var mSound: Double
        val mBuffer = ShortArray(duration)
        for (i in 0 until duration) {
            mSound = Math.sin(2.0 * Math.PI * (i.toDouble() / (sampleRate / frequency)))
            mBuffer[i] = (mSound * java.lang.Short.MAX_VALUE).toShort()
        }
        return mBuffer
    }
    @Suppress("DEPRECATION")
    private fun playSoundBuffer(mBuffer:ShortArray, onDone: () -> Unit = {}) {
        var minBufferSize = sampleRate/10
        if (minBufferSize < mBuffer.size) {
            minBufferSize += minBufferSize *
                    (Math.round(mBuffer.size.toFloat()) / minBufferSize.toFloat()).toInt()
        }

        val nBuffer = ShortArray(minBufferSize)
        for (i in nBuffer.indices) {
            if(i < mBuffer.size) nBuffer[i] = mBuffer[i]
            else nBuffer[i] = 0
        }

        val mAudioTrack = AudioTrack(AudioManager.STREAM_MUSIC, sampleRate,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                minBufferSize, AudioTrack.MODE_STREAM)

        mAudioTrack.setStereoVolume(AudioTrack.getMinVolume(), AudioTrack.getMaxVolume())
        mAudioTrack.notificationMarkerPosition = mBuffer.size
        Log.d("Progress", "After mAudioTrack")

        mAudioTrack.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
            override fun onPeriodicNotification(track: AudioTrack) {

            }

            override fun onMarkerReached(track: AudioTrack) {
                Log.d("Log", "Audio track end of file reached...")
                mAudioTrack.stop(); mAudioTrack.release(); onDone()
            }
        })
        Log.d("Progress", "After audio end")
        mAudioTrack.play(); mAudioTrack.write(nBuffer, 0, minBufferSize)
    }

    //End functions for beeps

    ////////////////////End Functions///////////////////

    //////////////////Button Functions//////////////////

    private fun showCodes(){
        appendTextAndScroll("Here are the codes")
        for(k in letToCodeDict.keys.sorted())
            appendTextAndScroll("$k: ${letToCodeDict[k]}")
    }

    // main output driver to mTextView
    private fun appendTextAndScroll(text: String) {
        if (mTextView != null) {
            mTextView.append(text + "\n")
            val layout = mTextView.layout
            if (layout != null) {
                val scrollDelta = (layout.getLineBottom(mTextView.lineCount - 1)
                        - mTextView.scrollY - mTextView.height)
                if(scrollDelta > 0)
                    mTextView.scrollBy(0, scrollDelta)
            }
        }
    }

    //Hides keyboard
    private fun Activity.hideKeyboard() {
        hideKeyboard(if (currentFocus == null) View(this) else currentFocus)
    }

    private fun Context.hideKeyboard(view: View) {
        val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }
    /////////////////End Button Functions/////////////////




    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


}
