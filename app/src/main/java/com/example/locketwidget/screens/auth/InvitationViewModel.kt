package com.example.locketwidget.screens.auth

import android.net.Uri
import com.airbnb.mvrx.*
import com.example.locketwidget.DeepLinkUseCase
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

data class InvitationState(
    val link: Async<Uri> = Uninitialized
) : MavericksState

class InvitationViewModel(
    initState: InvitationState,
    deepLinkUseCase: DeepLinkUseCase
) : MavericksViewModel<InvitationState>(initState) {
    init {
        viewModelScope.launch {
            val link = deepLinkUseCase.createLink(FirebaseAuth.getInstance().currentUser!!.uid)
            setState {
                if (link != null) copy(link = Success(link))
                else copy(link = Fail(NullPointerException()))
            }
        }
    }

    companion object : MavericksViewModelFactory<InvitationViewModel, InvitationState> {
        override fun create(viewModelContext: ViewModelContext, state: InvitationState): InvitationViewModel? {
            val deepLinkUseCase: DeepLinkUseCase by viewModelContext.activity.inject()
            return InvitationViewModel(state, deepLinkUseCase)
        }
    }
}