package nz.co.trademe.techtest.ui.util

import android.content.Context
import androidx.appcompat.app.AlertDialog
import nz.co.trademe.techtest.R
import retrofit2.HttpException
import java.io.IOException

/**
 * Class which provides shared error handling dialog utilities
 */
class ErrorDialogUtil {
	companion object {
		/**
		 * Provides basic error handling by displaying an error dialog to the user with a message depending on the [exception] type
		 * provided. The dialog only provides the user with the ability to retry, which can be handled through the [onRetry] callback.
		 */
		fun handleException(context: Context, exception: Throwable, onRetry: () -> Unit) {
			// basic error type checking
			val message = if (exception is HttpException || exception is IOException)
				R.string.network_error_message else R.string.unknown_error_message

			AlertDialog.Builder(context)
					.setMessage(message)
					// ensure user is forced to retry, as the screen is unusable without data
					.setCancelable(false)
					.setPositiveButton(R.string.retry) { _, _ ->
						onRetry()
					}
					.show()
		}
	}
}