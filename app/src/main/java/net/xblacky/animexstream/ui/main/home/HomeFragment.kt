package net.xblacky.animexstream.ui.main.home

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.transition.MaterialFadeThrough
import dagger.hilt.android.AndroidEntryPoint
import net.xblacky.animexstream.BuildConfig
import net.xblacky.animexstream.R
import net.xblacky.animexstream.databinding.FragmentHomeBinding
import net.xblacky.animexstream.ui.main.home.epoxy.HomeController
import net.xblacky.animexstream.utils.EventObserver
import net.xblacky.animexstream.utils.constants.C
import net.xblacky.animexstream.utils.model.AnimeMetaModel
import timber.log.Timber

@AndroidEntryPoint
class HomeFragment : Fragment(), View.OnClickListener, HomeController.EpoxyAdapterCallbacks {

    private lateinit var homeController: HomeController
    private var doubleClickLastTime = 0L

    private val viewModel: HomeViewModel by activityViewModels()

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupTransitions(view)

        setAdapter()
        setClickListeners()
        viewModel.queryDB()
        viewModelObserver()
        setupKeyListener()
    }

    private fun setupKeyListener() {
        // Set the key listener for the root view
        binding.root.setOnKeyListener { v, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_UP -> {
                        // Move focus to the element above the currently focused view
                        val viewAbove = v.focusSearch(View.FOCUS_UP)
                        viewAbove?.requestFocus()
                        return@setOnKeyListener true
                    }

                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                        // Move focus to the element below the currently focused view
                        val viewBelow = v.focusSearch(View.FOCUS_DOWN)
                        viewBelow?.requestFocus()
                        return@setOnKeyListener true
                    }

                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                        // Move focus to the element to the left of the currently focused view
                        val viewLeft = v.focusSearch(View.FOCUS_LEFT)
                        viewLeft?.requestFocus()
                        return@setOnKeyListener true
                    }

                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        // Move focus to the element to the right of the currently focused view
                        val viewRight = v.focusSearch(View.FOCUS_RIGHT)
                        viewRight?.requestFocus()
                        return@setOnKeyListener true
                    }
                }
            }
            return@setOnKeyListener false
        }
    }

    private fun setAdapter() {
        homeController = HomeController(this)
        homeController.isDebugLoggingEnabled = true
        homeController.setFilterDuplicates(true)
        val homeRecyclerView = binding.recyclerView
        homeRecyclerView.layoutManager = LinearLayoutManager(context)
        homeRecyclerView.adapter = homeController.adapter
    }

    private fun viewModelObserver() {
        viewModel.animeList.observe(viewLifecycleOwner) {
            homeController.setData(it)
        }
        viewModel.scrollToTopEvent.observe(viewLifecycleOwner, EventObserver {
            binding.recyclerView.smoothScrollToPosition(0)
        })

        viewModel.updateModel.observe(viewLifecycleOwner) {
            Timber.e(it.whatsNew)
            if (it.versionCode > BuildConfig.VERSION_CODE) {
                showDialog(it.whatsNew)
            }
        }
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
    }

    private fun setClickListeners() {
        binding.header.setOnClickListener(this)
        binding.search.setOnClickListener(this)
        binding.favorite.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.header -> {
                doubleClickLastTime = if (System.currentTimeMillis() - doubleClickLastTime < 300) {
                    binding.recyclerView.smoothScrollToPosition(0)
                    0L
                } else {
                    System.currentTimeMillis()
                }

            }

            R.id.search -> {
                val extras =
                    FragmentNavigatorExtras(binding.search to resources.getString(R.string.search_transition))
                findNavController().navigate(
                    HomeFragmentDirections.actionHomeFragmentToSearchFragment(),
                    extras
                )
            }

            R.id.favorite -> {
                val extras = FragmentNavigatorExtras(
                    binding.favorite to resources.getString(R.string.favourite_transition)

                )
                findNavController().navigate(
                    HomeFragmentDirections.actionHomeFragmentToFavouriteFragment(),
                    extras
                )
            }
        }
    }

    override fun recentSubDubEpisodeClick(model: AnimeMetaModel) {
        findNavController().navigate(
            HomeFragmentDirections.actionHomeFragmentToVideoPlayerActivity(
                episodeUrl = model.episodeUrl,
                animeName = model.title,
                episodeNumber = model.episodeNumber
            )
        )
    }

    override fun animeTitleClick(model: AnimeMetaModel, sharedTitle: View, sharedImage: View) {
        if (!model.categoryUrl.isNullOrBlank()) {

            val extras = FragmentNavigatorExtras(
                sharedTitle to resources.getString(R.string.shared_anime_title),
                sharedImage to resources.getString(R.string.shared_anime_image)
            )
            findNavController().navigate(
                HomeFragmentDirections.actionHomeFragmentToAnimeInfoFragment(
                    categoryUrl = model.categoryUrl!!,
                    animeImageUrl = model.imageUrl,
                    animeName = model.title
                ),
                extras
            )
        }

    }

    private fun showDialog(whatsNew: String) {
        AlertDialog.Builder(requireContext()).setTitle("New Update Available")
            .setMessage("What's New ! \n$whatsNew")
            .setCancelable(false)
            .setPositiveButton("Update") { _, _ ->
                val i = Intent(Intent.ACTION_VIEW)
                i.data = Uri.parse(C.GIT_DOWNLOAD_URL)
                startActivity(i)
            }
            .setNegativeButton("Not now") { dialog, _ ->
                dialog.cancel()
            }.show()
    }

}