package io.github.sds100.keymapper.ActionTypeFragments

import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.sds100.keymapper.Action
import io.github.sds100.keymapper.ActionType
import io.github.sds100.keymapper.Adapters.AppListAdapter
import io.github.sds100.keymapper.Adapters.SimpleItemAdapter
import io.github.sds100.keymapper.LoadAppListAsyncTask
import io.github.sds100.keymapper.R
import kotlinx.android.synthetic.main.action_type_recyclerview.*

/**
 * Created by sds100 on 29/07/2018.
 */

/**
 * A Fragment which shows a list of all the installed apps
 */
class AppActionTypeFragment : ActionTypeFragment(),
        SimpleItemAdapter.OnItemClickListener<ApplicationInfo> {

    private lateinit var mApps: List<ApplicationInfo>
    private var mAppListAdapter: AppListAdapter? = null

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.action_type_recyclerview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val packageManager = context!!.packageManager

        LoadAppListAsyncTask(
                packageManager,
                onResult = { result ->
                    mApps = result
                    if (mAppListAdapter == null) {
                        mAppListAdapter = AppListAdapter(
                                mApps,
                                packageManager = packageManager,
                                onItemClickListener = this@AppActionTypeFragment
                        )
                    }

                    //the task may be finished even if the fragment isn't showing
                    if (recyclerView != null) {
                        recyclerView.adapter = mAppListAdapter
                    }

                    if (progressBar != null) {
                        progressBar.visibility = View.GONE
                    }
                }).execute()

        progressBar.visibility = View.VISIBLE
        recyclerView.layoutManager = LinearLayoutManager(context!!)
    }

    override fun onItemClick(item: ApplicationInfo) {
        val action = Action(ActionType.APP, item.packageName)
        chooseSelectedAction(action)
    }
}