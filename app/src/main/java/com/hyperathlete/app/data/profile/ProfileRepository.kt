package com.hyperathlete.app.data.profile

import com.hyperathlete.app.data.db.UserProfileDao
import com.hyperathlete.app.data.db.UserProfileEntity
import com.hyperathlete.app.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(private val dao: UserProfileDao) {

    val profile: Flow<UserProfile> = dao.observe().map { it?.toDomain() ?: UserProfile() }

    suspend fun current(): UserProfile = dao.get()?.toDomain() ?: UserProfile()

    suspend fun save(profile: UserProfile) {
        dao.upsert(
            UserProfileEntity(
                name = profile.name.trim(),
                birthEpochDay = profile.birthDate?.toEpochDay(),
                weightKg = profile.weightKg,
                heightCm = profile.heightCm,
                photoUri = profile.photoUri,
            ),
        )
    }

    suspend fun update(transform: (UserProfile) -> UserProfile) = save(transform(current()))

    private fun UserProfileEntity.toDomain() = UserProfile(
        name = name,
        birthDate = birthEpochDay?.let(LocalDate::ofEpochDay),
        weightKg = weightKg,
        heightCm = heightCm,
        photoUri = photoUri,
    )
}
