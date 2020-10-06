package com.hyperana.kindleimagekeyboard

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager

class LaunchActivity : AppCompatActivity() {
    val TAG = "LaunchActivity"


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
            if (!nextFragment()) cb()
        }

        // remove fragment from pending list and show it until list is empty:
        private fun nextFragment() : Boolean{
            Log.d("FragmentChain", "nextFragment/${fragments.size}")

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
            Log.d("FragmentChain", "closeFragment")
            try {
                // replace with next:
                if (!nextFragment()) {

                    // or simply remove and report done:
                    manager.beginTransaction()
                        .remove(fragment)
                        .commit()
                    cb()
                }
            }
            catch (e: Exception) {
                Log.e("FragmentChainListener", "failed close chain fragment", e)
            }

        }
    }




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        // run through startup actions, launch next activity, and then finish:
        FragmentChainListener(R.id.loading_fragment_view, supportFragmentManager) {
            Log.i(TAG, "startup fragments finished, starting MainActivity")
            startActivity(Intent(this, MainActivity::class.java))
            //finish()
        }.also { chain ->
            val startup = mutableListOf<Fragment>()

            startup.add(LoadAssetsFragment.create(chain))
            startup.add(ChooseKeyboardFragment.create(chain))

            //todo: -?- add fragment to load images for current aac keyboard/page
            chain.start(startup)
        }

    }
}