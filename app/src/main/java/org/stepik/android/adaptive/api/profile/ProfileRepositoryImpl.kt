package org.stepik.android.adaptive.api.profile

import io.reactivex.Completable
import io.reactivex.Single
import org.stepik.android.adaptive.api.profile.model.ChangePasswordRequest
import org.stepik.android.adaptive.api.profile.model.EmailAddress
import org.stepik.android.adaptive.api.profile.model.EmailAddressRequest
import org.stepik.android.adaptive.api.profile.model.ProfileRequest
import org.stepik.android.adaptive.data.model.Profile
import org.stepik.android.adaptive.di.AppSingleton
import org.stepik.android.adaptive.util.then
import javax.inject.Inject

@AppSingleton
class ProfileRepositoryImpl
@Inject
constructor(
        private val profileService: ProfileService
): ProfileRepository {

    override fun fetchProfile(): Single<Profile> =
            profileService.profile.map { it.profile!! }

    override fun updateProfile(profile: Profile): Completable =
            profileService.setProfile(profile.id, ProfileRequest(profile))

    override fun updatePassword(profileId: Long, oldPassword: String, newPassword: String): Completable =
            profileService.changePassword(profileId, ChangePasswordRequest(currentPassword = oldPassword, newPassword = newPassword))

    override fun updateEmail(newEmail: String): Completable = fetchProfile().flatMapCompletable { profile ->
        val replaceAddressRx =
                profileService.createEmailAddress(EmailAddressRequest(EmailAddress(user = profile.id, email = newEmail))).flatMapCompletable {
                    profileService.setEmailAsPrimary(it.emailAddress?.id ?: 0)
                }

        val oldEmailId = profile.emailAddresses?.firstOrNull()
        if (oldEmailId != null) {
            replaceAddressRx then profileService.removeEmailAddress(oldEmailId)
        } else {
            replaceAddressRx
        }
    }

}