package talos.laws

import cats.effect.{Effect}
import talos.circuitbreakers.TalosCircuitBreaker

import scala.concurrent.duration._

private[laws] trait CircuitBreakerSpec[C, S, F[_]] {
  val talosCircuitBreaker: TalosCircuitBreaker[C, S, F]

  final val callTimeout: FiniteDuration = 2 seconds
  final val resetTimeout: FiniteDuration = callTimeout*3

  private[laws] def run[A](unprotectedCall: F[A])(implicit F: Effect[F]): A = F.toIO(
    talosCircuitBreaker.protect(unprotectedCall)
  ).unsafeRunSync()

  private[laws] def runWithFallback[A, E](unprotectedCall: F[A], fallback: F[E])(implicit F: Effect[F]): Either[E, A] =
    F.toIO(talosCircuitBreaker
      .protectWithFallback(unprotectedCall, fallback))
    .unsafeRunSync()

}
