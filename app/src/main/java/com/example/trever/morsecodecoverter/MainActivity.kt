package com.example.trever.morsecodecoverter

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.text.method.ScrollingMovementMethod
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

    private fun convertMorseToLet(text: String){
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
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }


}
