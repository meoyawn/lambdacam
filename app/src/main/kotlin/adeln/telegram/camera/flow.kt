package adeln.telegram.camera

import android.app.Activity
import android.os.Bundle
import android.os.Parcelable
import flow.Flow
import flow.FlowDelegate
import flow.History
import flow.StateParceler

fun <T> History.push(t: T): History =
    buildUpon().push(t).build()

fun <T> Flow.push(t: T): Unit =
    setHistory(history.push(t), Flow.Direction.FORWARD)

fun <T> Flow.resetTo(t: T): Unit =
    setHistory(History.single(t), Flow.Direction.REPLACE)

inline fun <reified T> History.backAndReplace(replace: (T) -> T): History {
  val b = buildUpon()
  b.pop()
  val toReplace = b.pop() as T
  val replacement = replace(toReplace)
  return b.push(replacement).build()
}

fun <T> History.replace(t: T): History =
    buildUpon().apply { pop() }.push(t).build()

fun <T> Flow.replace(t: T): Unit =
    setHistory(history.replace(t), Flow.Direction.REPLACE)

inline fun <reified T> Flow.backAndReplace(replace: (T) -> T): Unit =
    setHistory(history.backAndReplace(replace), Flow.Direction.BACKWARD)

@Suppress("DEPRECATION")
fun Activity.mkFlow(parceler: StateParceler,
                    initial: History,
                    state: Bundle?,
                    dispatcher: (Flow.Traversal, Flow.TraversalCallback) -> Unit): FlowDelegate =
    FlowDelegate.onCreate(lastNonConfigurationInstance as FlowDelegate.NonConfigurationInstance?,
                          intent,
                          state,
                          parceler,
                          initial,
                          dispatcher)

object ParcelableParceler : StateParceler {
  override fun unwrap(parcelable: Parcelable): Any = parcelable
  override fun wrap(instance: Any): Parcelable = instance as Parcelable
}
