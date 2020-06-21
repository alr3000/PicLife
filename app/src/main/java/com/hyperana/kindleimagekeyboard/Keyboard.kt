package com.hyperana.kindleimagekeyboard

import java.util.*

/**
 * Created by alr on 12/1/17.
 */
class Keyboard(list: List<PageData>, name: String) : ArrayList<PageData>(list) {

    val TAG = "Keyboard"
    val lastAccess: Long = Date().time


}