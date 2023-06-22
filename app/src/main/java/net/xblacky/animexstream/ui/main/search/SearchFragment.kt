package net.xblacky.animexstream.ui.main.search

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView.OnEditorActionListener
import androidx.core.content.ContextCompat
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialContainerTransform
import com.google.android.material.transition.MaterialFadeThrough
import kotlinx.android.synthetic.main.fragment_search.view.searchEditText
import kotlinx.android.synthetic.main.fragment_search.view.searchRecyclerView
import kotlinx.android.synthetic.main.loading.view.loading
import net.xblacky.animexstream.R
import net.xblacky.animexstream.ui.main.search.epoxy.SearchController
import net.xblacky.animexstream.utils.CommonViewModel2
import net.xblacky.animexstream.utils.ItemOffsetDecoration
import net.xblacky.animexstream.utils.Utils
import net.xblacky.animexstream.utils.model.AnimeMetaModel


class SearchFragment : Fragment(), SearchController.EpoxySearchAdapterCallbacks {

    private lateinit var rootView: View
    private lateinit var viewModel: SearchViewModel
    private lateinit var searchController: SearchController
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        rootView = inflater.inflate(R.layout.fragment_search, container, false)
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this).get(SearchViewModel::class.java)

        setupTransitions(view)
        setObserver()
        setAdapters()
        setRecyclerViewScroll()
        setEditTextListener()
    }


    private fun setEditTextListener() {
        rootView.searchEditText.setOnEditorActionListener(OnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyBoard()
                rootView.searchEditText.clearFocus()
                viewModel.fetchSearchList(v.text.toString().trim())
                return@OnEditorActionListener true
            }
            false
        })
        rootView.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.fetchSearchList(s.toString().trim())
            }

            override fun afterTextChanged(s: Editable?) {

            }

        })
    }

    private fun setAdapters() {
        searchController = SearchController(this)
        searchController.spanCount = Utils.calculateNoOfColumns(requireContext(), 150f)
        rootView.searchRecyclerView.apply {
            layoutManager =
                GridLayoutManager(context, Utils.calculateNoOfColumns(requireContext(), 150f))
            adapter = searchController.adapter
            (layoutManager as GridLayoutManager).spanSizeLookup = searchController.spanSizeLookup
        }
        rootView.searchRecyclerView.addItemDecoration(
            ItemOffsetDecoration(
                context,
                R.dimen.episode_offset_left
            )
        )

    }

    private fun getSpanCount(): Int {
        val orientation = resources.configuration.orientation
        return if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            5
        } else {
            3
        }
    }

    private fun setObserver() {


        viewModel.loadingModel.observe(viewLifecycleOwner, Observer {
            if (it.isListEmpty) {
                if (it.loading == CommonViewModel2.Loading.LOADING) rootView.loading.visibility =
                    View.VISIBLE
                //TODO Error Visibiity GONE

                else if (it.loading == CommonViewModel2.Loading.ERROR
                //Todo Error visisblity visible
                ) rootView.loading.visibility = View.GONE
            } else {
                searchController.setData(
                    viewModel.searchList.value,
                    it.loading == CommonViewModel2.Loading.LOADING
                )
                if (it.loading == CommonViewModel2.Loading.ERROR) view?.let { it1 ->
                    Snackbar.make(
                        it1,
                        getString(it.errorMsg),
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
                else if (it.loading == CommonViewModel2.Loading.COMPLETED) rootView.loading.visibility =
                    View.GONE

            }

        })

//        viewModel.searchList.observe(viewLifecycleOwner, Observer {
//            searchController.setData(it ,viewModel.isLoading.value?.isLoading ?: false)
//            if(!it.isNullOrEmpty()){
//                hideKeyBoard()
//            }
//        })
//
//
//        viewModel.isLoading.observe( viewLifecycleOwner, Observer {
//            if(it.isLoading){
//                if(it.isListEmpty){
//                    rootView.loading.visibility =  View.VISIBLE
//                }else{
//                    rootView.loading.visibility = View.GONE
//                }
//            }else{
//               rootView.loading.visibility = View.GONE
//            }
//            searchController.setData(viewModel.searchList.value, it.isLoading)
//        })
    }

    private fun setRecyclerViewScroll() {
        rootView.searchRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManger = rootView.searchRecyclerView.layoutManager as GridLayoutManager
                val visibleItemCount = layoutManger.childCount
                val totalItemCount = layoutManger.itemCount
                val firstVisibleItemPosition = layoutManger.findFirstVisibleItemPosition()

                if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                    && firstVisibleItemPosition >= 0
                ) {
                    if (isNetworkAvailable()) {
                        viewModel.fetchNextPage()
                    } else {
                        Snackbar.make(
                            view!!,
                            getString(R.string.no_internet),
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        })
    }

    private fun hideKeyBoard() {
        val imm =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(requireActivity().currentFocus?.windowToken, 0)
    }

    private fun showKeyBoard() {
        val imm =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(requireActivity().currentFocus, 0)
    }

    override fun animeTitleClick(model: AnimeMetaModel, sharedTitle: View, sharedImage: View) {
        val extras = FragmentNavigatorExtras(
            sharedTitle to resources.getString(R.string.shared_anime_title),
            sharedImage to resources.getString(R.string.shared_anime_image)
        )
        findNavController().navigate(
            SearchFragmentDirections.actionSearchFragmentToAnimeInfoFragment(
                categoryUrl = model.categoryUrl!!,
                animeImageUrl = model.imageUrl,
                animeName = model.title
            ),
            extras
        )
    }

    private fun setupTransitions(view: View) {
        postponeEnterTransition()
        view.doOnPreDraw { startPostponedEnterTransition() }
        exitTransition = MaterialFadeThrough().apply {
            duration = 300
        }
        reenterTransition = MaterialFadeThrough().apply {
            duration = 300
        }
        sharedElementEnterTransition = MaterialContainerTransform().apply {
            drawingViewId = R.id.navHostFragmentContainer
            duration = 300
            scrimColor = Color.TRANSPARENT
            fadeMode = MaterialContainerTransform.FADE_MODE_THROUGH
            startContainerColor = ContextCompat.getColor(view.context, android.R.color.transparent)
            endContainerColor = ContextCompat.getColor(view.context, android.R.color.transparent)
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = activity?.getSystemService(Context.CONNECTIVITY_SERVICE)
        return if (connectivityManager is ConnectivityManager) {
            val networkInfo: NetworkInfo? = connectivityManager.activeNetworkInfo
            networkInfo?.isConnected ?: false
        } else false
    }

}