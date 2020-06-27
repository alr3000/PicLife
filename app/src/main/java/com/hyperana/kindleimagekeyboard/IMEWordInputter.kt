package com.hyperana.kindleimagekeyboard

import android.inputmethodservice.InputMethodService
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedText
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputConnection

class IMEWordInputter (val ime: InputMethodService)
    : WordInputter() {


    var editorActionId = null

    // limit=0 allows trailing empty strings to represent end-of-string split
    fun getWordBeforeCursor(ic: InputConnection) : String {
        val arr = splitWords(ic.getTextBeforeCursor(255, 0))
        Log.d(TAG, "words before: " + arr.joinToString(","))
        return if (arr.isEmpty()) "" else arr.last()
    }

    fun getWordAfterCursor(ic: InputConnection) : String {
        val arr = splitWords(ic.getTextAfterCursor(255, 0))
        Log.d(TAG, "words after: " + arr.joinToString(","))
        return if (arr.isEmpty()) "" else arr.first()
    }

    fun getNextWord(ic: InputConnection) : RelativeWord? {
        val t =  word.find(ic.getTextAfterCursor(255, 0), 0)
        return if (t == null) null
        else RelativeWord(t.value, t.range.start, t.range.endInclusive)
    }

    fun getPreviousWord(ic: InputConnection) : RelativeWord? {
        val text = ic.getTextBeforeCursor(255, 0)
        val t =  word.findAll(text, 0).lastOrNull()
        return if (t==null) null
        else RelativeWord(t.value, 0 - (text.length - t.range.start), 0 - (text.length - t.range.endInclusive))
    }

    fun getCurrentWord(ic: InputConnection) : RelativeWord? {
        val forward = getNextWord(ic)
        val backward = getPreviousWord(ic)

        Log.d(TAG, "currentWord finds " + backward.toString() + forward.toString())
        val out = RelativeWord()
        if ((backward == null) || (backward.endInclusive < -1)) {
            return null
        }
        else {
            out.start = backward.start
            out.text = backward.text
        }

        if ((forward == null) || (forward.start > 0)) {
            return null
        }
        else {
            out.endInclusive = forward.endInclusive
            out.text += forward.text
        }
        Log.d(TAG, "currentWord returns " + out.toString())
        return out
    }

    fun getCursorPosition(ic: InputConnection) : Int {
        val extracted: ExtractedText = ic.getExtractedText(ExtractedTextRequest(), 0);
        return extracted.startOffset + extracted.selectionStart;
    }

    override fun getAllText() : String {
        return (ime.currentInputConnection?.getTextBeforeCursor(9999, 0)?.trim() ?: "").toString()
            .plus(ime.currentInputConnection?.getTextAfterCursor(9999, 0)?.trim() ?: "")
    }


    override fun input(text: String) {
        try {
            val ic = ime.currentInputConnection
            val backward = getWordBeforeCursor(ic)
            val forward = getWordAfterCursor(ic)

            Log.d(TAG, "cursor position: " + getCursorPosition(ic))
            Log.d(TAG, "found adjacent text: [" + backward + "_" + forward + "]")

            // if in the middle of a word, delete it
            if (forward.isNotEmpty() && backward.isNotEmpty()) {
                //editor may not respond to this call
                //ic.commitCorrection(CorrectionInfo(getCursorPosition()-backward.length, backward + forward, icon.text))
                ic.deleteSurroundingText(backward.length, forward.length)
                ic.commitText(text, 1)
            } else {
                // add word with spaces if necessary
                val text = (if (backward.isNotEmpty()) " " else "") +
                        text + (if (forward.isNotEmpty()) " " else "")
                Log.d(TAG, "commit text: [" + text + "]")
                ic.commitText(text, 1)
            }

            update()
        } catch(e: Exception) {
            Log.e(TAG, "input problem: " + e.message)
        }
    }


    override fun forwardDelete() {
        try {
            val ic = ime.currentInputConnection
            val nextWord = getCurrentWord(ic) ?: getNextWord(ic)
            Log.d(TAG, "forwardDelete word: " + nextWord.toString())
            if (nextWord == null) {
                return
            }
            if (nextWord.start >= 0) {
                ic.deleteSurroundingText(0, nextWord.endInclusive + 1)
            } else {
                ic.deleteSurroundingText(Math.abs(nextWord.start), nextWord.endInclusive + 1)
            }
            update()
        }
        catch(e: Exception) {
            Log.e(TAG, "input problem: " + e.message)
        }
    }

    override fun backwardDelete() {
        try {
            val ic = ime.currentInputConnection

            val prevWord = getCurrentWord(ic) ?: getPreviousWord(ic)
            Log.d(TAG, "backwardDelete word: " + prevWord.toString())
            if (prevWord == null) {
                return
            }
            if (prevWord.endInclusive < 0) {
                ic.deleteSurroundingText(Math.abs(prevWord.start), 0)
            }
            else {
                ic.deleteSurroundingText(Math.abs(prevWord.start), prevWord.endInclusive + 1)
            }
            update()
        }
        catch(e: Exception) {
            Log.e(TAG, "input problem: " + e.message)
        }
    }

    override fun action() {
            Log.d(TAG, "call performEditorAction "+ editorActionId+" on DONE")

            update()
            //currentEditorInfo?.imeOptions?.and(EditorInfo.IME_FLAG_NO_ENTER_ACTION)
            ime.currentInputConnection.performEditorAction(editorActionId ?: 0)
    }
}