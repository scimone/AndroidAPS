package info.nightscout.androidaps.plugins.aps.loop


import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import info.nightscout.androidaps.MainApp
import info.nightscout.androidaps.R
import info.nightscout.androidaps.interfaces.Constraint
import info.nightscout.androidaps.plugins.aps.loop.events.EventLoopSetLastRunGui
import info.nightscout.androidaps.plugins.aps.loop.events.EventLoopUpdateGui
import info.nightscout.androidaps.plugins.bus.RxBus
import info.nightscout.androidaps.utils.DateUtil
import info.nightscout.androidaps.utils.FabricPrivacy
import info.nightscout.androidaps.utils.HtmlHelper
import info.nightscout.androidaps.utils.SP
import info.nightscout.androidaps.utils.plusAssign
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.loop_fragment.*
import java.util.*

class LoopFragment : Fragment() {
    private lateinit var mRandom: Random
    private lateinit var mHandler: Handler
    private lateinit var mRunnable:Runnable
    private var disposable: CompositeDisposable = CompositeDisposable()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.loop_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        swipeRefresh_loop.setColorSchemeResources(R.color.orange, R.color.green, R.color.blue)
        swipeRefresh_loop.setProgressBackgroundColorSchemeColor(ResourcesCompat.getColor(resources, R.color.swipe_background, null))

        // Initialize a new Random instance
        mRandom = Random()

        // Initialize the handler instance
        mHandler = Handler()

            swipeRefresh_loop.setOnRefreshListener {
            mRunnable = Runnable {
                loop_lastrun.text = MainApp.gs(R.string.executing)
                Thread { LoopPlugin.getPlugin().invoke("Loop button", true) }.start()
                // Hide swipe to refresh icon animation
                swipeRefresh_loop.isRefreshing = false
            }

            // Execute the task after specified time
            mHandler.postDelayed(
                mRunnable,
                (3000).toLong() // Delay 1 to 5 seconds
            )
        }

    }

    @Synchronized
    override fun onResume() {
        super.onResume()
        disposable += RxBus
                .toObservable(EventLoopUpdateGui::class.java)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    updateGUI()
                }, {
                    FabricPrivacy.logException(it)
                })

        disposable += RxBus
                .toObservable(EventLoopSetLastRunGui::class.java)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    clearGUI()
                    loop_lastrun.text = it.text
                }, {
                    FabricPrivacy.logException(it)
                })

        updateGUI()
        SP.putBoolean(R.string.key_objectiveuseloop, true)
    }

    @Synchronized
    override fun onPause() {
        super.onPause()
        disposable.clear()
    }

    @Synchronized
    fun updateGUI() {
        if (loop_request == null) return
        LoopPlugin.lastRun?.let {
            loop_request.text = it.request?.toSpanned() ?: ""
            loop_constraintsprocessed.text = it.constraintsProcessed?.toSpanned() ?: ""
            loop_source.text = it.source ?: ""
            loop_lastrun.text = it.lastAPSRun?.let { lastRun -> DateUtil.dateAndTimeString(lastRun.time) }
                    ?: ""
            loop_lastenact.text = it.lastAPSRun?.let { lastEnact -> DateUtil.dateAndTimeString(lastEnact.time) }
                    ?: ""
            loop_tbrsetbypump.text = it.tbrSetByPump?.let { tbrSetByPump -> HtmlHelper.fromHtml(tbrSetByPump.toHtml()) }
                    ?: ""
            loop_smbsetbypump.text = it.smbSetByPump?.let { smbSetByPump -> HtmlHelper.fromHtml(smbSetByPump.toHtml()) }
                    ?: ""

            val constraints =
                    it.constraintsProcessed?.let { constraintsProcessed ->
                        val allConstraints = Constraint(0.0)
                        constraintsProcessed.rateConstraint?.let { rateConstraint -> allConstraints.copyReasons(rateConstraint) }
                        constraintsProcessed.smbConstraint?.let { smbConstraint -> allConstraints.copyReasons(smbConstraint) }
                        allConstraints.mostLimitedReasons
                    } ?: ""
            loop_constraints.text = constraints
        }
    }

    @Synchronized
    private fun clearGUI() {
        if (loop_request == null) return
        loop_request.text = ""
        loop_constraints.text = ""
        loop_constraintsprocessed.text = ""
        loop_source.text = ""
        loop_lastrun.text = ""
        loop_lastenact.text = ""
        loop_tbrsetbypump.text = ""
        loop_smbsetbypump.text = ""
    }
}
