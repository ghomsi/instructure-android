/*
 * Copyright (C) 2017 - present Instructure, Inc.
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, version 3 of the License.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.instructure.teacher.fragments

import android.graphics.Color
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import com.instructure.canvasapi2.models.CanvasContext
import com.instructure.canvasapi2.models.Page
import com.instructure.canvasapi2.utils.ApiPrefs
import com.instructure.pandarecycler.util.UpdatableSortedList
import com.instructure.pandautils.fragments.BaseSyncFragment
import com.instructure.pandautils.utils.*
import com.instructure.pandautils.utils.ColorKeeper
import com.instructure.pandautils.utils.ColorUtils
import com.instructure.pandautils.utils.ThemePrefs
import com.instructure.pandautils.utils.ViewStyler
import com.instructure.teacher.R
import com.instructure.teacher.adapters.PageListAdapter
import com.instructure.teacher.events.PageCreatedEvent
import com.instructure.teacher.events.PageDeletedEvent
import com.instructure.teacher.events.PageUpdatedEvent
import com.instructure.teacher.factory.PageListPresenterFactory
import com.instructure.teacher.holders.PageViewHolder
import com.instructure.teacher.presenters.PageListPresenter
import com.instructure.interactions.router.Route
import com.instructure.teacher.router.RouteMatcher
import com.instructure.teacher.utils.*
import com.instructure.teacher.viewinterface.PageListView
import instructure.androidblueprint.PresenterFactory
import kotlinx.android.synthetic.main.fragment_page_list.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class PageListFragment : BaseSyncFragment<Page, PageListPresenter, PageListView, PageViewHolder, PageListAdapter>(), PageListView {

    private var mCanvasContext: CanvasContext by ParcelableArg(default = CanvasContext.getGenericContext(CanvasContext.Type.COURSE, -1L, ""))

    private var mLinearLayoutManager = LinearLayoutManager(context)
    lateinit private var mRecyclerView: RecyclerView
    private val mCourseColor by lazy { ColorKeeper.getOrGenerateColor(mCanvasContext) }

    private var mNeedToForceNetwork = false

    override fun layoutResId(): Int = R.layout.fragment_page_list
    override fun getRecyclerView(): RecyclerView = pageRecyclerView
    override fun getPresenterFactory(): PresenterFactory<PageListPresenter> = PageListPresenterFactory(mCanvasContext)
    override fun onPresenterPrepared(presenter: PageListPresenter?) {
        mRecyclerView = RecyclerViewUtils.buildRecyclerView(mRootView, context, adapter, presenter, R.id.swipeRefreshLayout,
                R.id.pageRecyclerView, R.id.emptyPandaView, getString(R.string.no_items_to_display_short))

        mRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0 && createNewPage.visibility == View.VISIBLE) {
                    createNewPage.hide()
                } else if (dy < 0 && createNewPage.visibility != View.VISIBLE) {
                    createNewPage.show()
                }
            }
        })

        setupViews()
    }

    override fun getList(): UpdatableSortedList<Page> = presenter.data


    override fun onCreateView(view: View) {
        mLinearLayoutManager.orientation = LinearLayoutManager.VERTICAL
    }

    override fun onReadySetGo(presenter: PageListPresenter) {
        if(recyclerView.adapter == null) {
            mRecyclerView.adapter = adapter
        }
        presenter.loadData(mNeedToForceNetwork)
    }

    override fun onResume() {
        super.onResume()
        setupToolbar()
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    override fun getAdapter(): PageListAdapter {
        if (mAdapter == null) {
            mAdapter = PageListAdapter(context, presenter, mCourseColor) { page ->
                val args = PageDetailsFragment.makeBundle(page)
                RouteMatcher.route(context, Route(null, PageDetailsFragment::class.java, mCanvasContext, args))
            }
        }
        return mAdapter
    }


    override fun onRefreshStarted() {
        createNewPage.setGone()
        //this prevents two loading spinners from happening during pull to refresh
        if(!swipeRefreshLayout.isRefreshing) {
            emptyPandaView.visibility  = View.VISIBLE
        }
        emptyPandaView.setLoading()
    }

    override fun onRefreshFinished() {
        swipeRefreshLayout.isRefreshing = false
        createNewPage.setVisible()
    }

    override fun checkIfEmpty() {
        // We don't want to leave the fab hidden if the list is empty
        if(presenter.isEmpty) {
            createNewPage.show()
        }

        RecyclerViewUtils.checkIfEmpty(emptyPandaView, mRecyclerView, swipeRefreshLayout, adapter, presenter.isEmpty)
    }

    override fun perPageCount() = ApiPrefs.perPageCount

    private fun setupToolbar() {
        pageListToolbar.title = getString(R.string.tab_pages)
        pageListToolbar.subtitle = mCanvasContext.name
        pageListToolbar.setupBackButton(this)

        ViewStyler.themeToolbar(activity, pageListToolbar, mCourseColor, Color.WHITE)
    }

    private fun setupViews() {
        createNewPage.setGone()
        createNewPage.backgroundTintList = ViewStyler.makeColorStateListForButton()
        createNewPage.setImageDrawable(ColorUtils.colorIt(ThemePrefs.buttonTextColor, createNewPage.drawable))
        createNewPage.onClickWithRequireNetwork {
            val args = CreateOrEditPageDetailsFragment.newInstanceCreate(mCanvasContext).arguments
            RouteMatcher.route(context, Route(CreateOrEditPageDetailsFragment::class.java, null, args))
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onPageCreated(event: PageCreatedEvent) {
        event.once(javaClass.simpleName) {
            // need to set a flag here. Because we use the event bus in the fragment instead of the presenter for unit testing purposes,
            // when we come back to this fragment it will go through the life cycle events again and the cached data will immediately
            // overwrite the data from the network if we refresh the presenter from here.
            mNeedToForceNetwork = true
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onPageUpdated(event: PageUpdatedEvent) {
        event.once(javaClass.simpleName) {
            // need to set a flag here. Because we use the event bus in the fragment instead of the presenter for unit testing purposes,
            // when we come back to this fragment it will go through the life cycle events again and the cached data will immediately
            // overwrite the data from the network if we refresh the presenter from here.
            mNeedToForceNetwork = true
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onPageDeleted(event: PageDeletedEvent) {
        event.get {
            adapter.remove(it)
        }
    }

    companion object {

        @JvmStatic
        fun newInstance(canvasContext: CanvasContext) = PageListFragment().apply {
            mCanvasContext = canvasContext
        }
    }
}
