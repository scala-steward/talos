package talos.laws

import cats.effect.Effect
import org.scalatest.Matchers
import talos.events.TalosEvents.model._
import org.scalacheck.Gen

import scala.util.Try

trait FallbackLaws[S, C, F[_]] extends EventBusLaws[S] with CircuitBreakerSpec[C, F] with Matchers {

  private[laws] def fallbackActivatedOnError(implicit F: Effect[F]) = {
    val error = Gen.alphaNumStr
    runWithFallback(F.raiseError(new RuntimeException), F.pure(error)) should matchPattern {
      case Left(err) if error == err =>
    }
    acceptMsg.isInstanceOf[CallFailure]
    acceptMsg shouldBe FallbackSuccess(talosCircuitBreaker.name)
  }

  private[laws] def fallbackFailureIsLogged(implicit F: Effect[F]) = {
    Try(runWithFallback(F.raiseError(new RuntimeException), F.raiseError(new IllegalStateException)))
    acceptMsg.isInstanceOf[CallFailure]
    acceptMsg shouldBe FallbackFailure(talosCircuitBreaker.name)
  }

  private[laws] def fallbackExpected(implicit F: Effect[F]) = {
    val error = Gen.alphaNumStr
    runWithFallback(F.unit, F.pure(error)) should matchPattern {
      case Left(err) if error == err =>
    }
    acceptMsg match {
      case ShortCircuitedCall(name) if name == talosCircuitBreaker.name =>
        acceptMsg shouldBe FallbackSuccess(talosCircuitBreaker.name)
      case FallbackSuccess(name) if name == talosCircuitBreaker.name =>
        acceptMsg shouldBe ShortCircuitedCall(talosCircuitBreaker.name)
      case other => fail(s"Unexpected circuit breaker event $other")
    }

  }
}
