package com.hyperana.kindleimagekeyboard

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager

class LaunchActivity : AppCompatActivity() {
    val TAG = this::class.java.name


    //todo: add view with progress bar
    class FragmentChainListener (
        val containerId: Int,
        val manager: FragmentManager,
        val cb: () -> Unit
    )
        : FragmentListener {

        private var fragments = mutableListOf<Fragment>()

        fun start(fragmentList: List<Fragment>) {
            fragments = fragmentList.toMutableList()
            nextFragment()
        }

        private fun nextFragment() : Boolean{
            if (fragments.isNotEmpty())
                fragments.removeAt(0).also {
                    manager.beginTransaction()
                        .replace(containerId, it)
                        .commit()
                    return true
                }
            else return false
        }

        override fun closeFragment(fragment: Fragment) {
            try {
                if (!nextFragment()) {
                    manager.beginTransaction()
                        .remove(fragment)
                        .commit()
                }
            }
            catch (e: Exception) {
                Log.e("FragmentChainListener", "failed close chain fragment", e)
            }
            finally {
                cb()
            }
        }
    }




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        // run through startup actions, launch next activity, and then finish:
        FragmentChainListener(R.id.loading_fragment_view, supportFragmentManager) {
            finish()
        }.also { chain ->
                val startup = mutableListOf<Fragment>()
                if (getKeyboardsNotLoaded(this).isNotEmpty())
                    startup.add(LoadAssetsFragment.create(chain))

                //todo: -?- add fragment to load images for current aac keyboard/page
                chain.start(startup)
            }

    }
}