package com.yourssu.ssutime.v2.component

import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity

/**
 * ModalBottomSheet 내에서 위로 스크롤/드래그할 때 발생하는
 * 중첩 스크롤 충돌(바운싱 및 지지직거림 현상)을 방지하는 Modifier
 */
fun Modifier.preventBottomSheetJitter(): Modifier = composed {
    val connection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // 위로 드래그할 때(available.y < 0) 리스트가 더 이상 스크롤할 수 없어 남은 오프셋을 소비하여
                // ModalBottomSheet로 전달되어 발생하는 지지직거림/바운싱을 방지
                return if (available.y < 0f) {
                    Offset(x = 0f, y = available.y)
                } else {
                    Offset.Zero
                }
            }

            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity
            ): Velocity {
                // 위로 빠르게 플링할 때 남은 속도를 소비하여 시트 진동 방지
                return if (available.y < 0f) {
                    Velocity(x = 0f, y = available.y)
                } else {
                    Velocity.Zero
                }
            }
        }
    }
    this.nestedScroll(connection)
}
