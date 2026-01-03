package uk.akane.accord.ui.fragments.browse

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uk.akane.accord.R
import uk.akane.accord.ui.MainActivity
import uk.akane.libphonograph.manipulator.ItemManipulator

class AddPlaylistFragment : Fragment() {

    private lateinit var cancelButton: TextView
    private lateinit var createButton: TextView
    private lateinit var titleInput: EditText
    private lateinit var coverCard: MaterialCardView
    private lateinit var coverAction: View
    private lateinit var coverImage: ImageView
    private var coverUri: Uri? = null
    private var backCallback: OnBackPressedCallback? = null

    private val coverPicker =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@registerForActivityResult
            requireContext().contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            coverUri = uri
            coverImage.setImageURI(uri)
            coverImage.isVisible = true
            coverAction.isVisible = false
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val rootView = inflater.inflate(R.layout.fragment_add_playlist, container, false)

        cancelButton = rootView.findViewById(R.id.btn_cancel)
        createButton = rootView.findViewById(R.id.btn_create)
        titleInput = rootView.findViewById(R.id.title_input)
        coverCard = rootView.findViewById(R.id.cover_card)
        coverAction = rootView.findViewById(R.id.cover_action)
        coverImage = rootView.findViewById(R.id.cover_image)

        setCreateEnabled(false)

        cancelButton.setOnClickListener {
            hideKeyboard()
            (requireActivity() as MainActivity).removeContainer()
        }

        createButton.setOnClickListener {
            if (!createButton.isEnabled) return@setOnClickListener
            createPlaylist()
        }

        titleInput.addTextChangedListener {
            setCreateEnabled(!it.isNullOrBlank())
        }

        coverCard.setOnClickListener {
            coverPicker.launch(arrayOf("image/*"))
        }

        backCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                hideKeyboard()
                (requireActivity() as MainActivity).removeContainer()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backCallback!!)

        return rootView
    }

    override fun onDestroyView() {
        backCallback = null
        hideKeyboard()
        super.onDestroyView()
    }

    private fun setCreateEnabled(enabled: Boolean) {
        createButton.isEnabled = enabled
        val color = if (enabled) {
            requireContext().getColor(R.color.accentColor)
        } else {
            requireContext().getColor(R.color.onSurfaceColorInactive)
        }
        createButton.setTextColor(color)
    }

    private fun createPlaylist() {
        val name = titleInput.text?.toString()?.trim().orEmpty()
        if (name.isEmpty()) return

        setCreateEnabled(false)
        lifecycleScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    ItemManipulator.createPlaylist(requireContext(), name)
                }
            }

            if (result.isFailure) {
                setCreateEnabled(true)
                Toast.makeText(
                    requireContext(),
                    "Failed to create playlist",
                    Toast.LENGTH_SHORT
                ).show()
                return@launch
            }

            coverUri?.let { uri ->
                val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().putString(coverKey(name), uri.toString()).apply()
            }

            hideKeyboard()
            val activity = requireActivity() as MainActivity
            activity.updateLibrary()
            activity.removeContainer()
        }
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(titleInput.windowToken, 0)
        titleInput.clearFocus()
    }

    private fun coverKey(name: String): String = "$COVER_KEY_PREFIX$name"

    companion object {
        private const val PREFS_NAME = "playlist_covers"
        private const val COVER_KEY_PREFIX = "playlist_cover_"
    }
}
