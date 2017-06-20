package com.hubspot.singularity.mesos;

import java.util.Collections;
import java.util.List;

import org.apache.mesos.v1.Protos.Offer;
import org.apache.mesos.v1.Protos.OfferID;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.mesos.SingularityOfferCache.CachedOffer;

@Singleton
public class SingularityNoOfferCache implements OfferCache {

  private final SingularityDriver singularityDriver;

  @Inject
  public SingularityNoOfferCache(SingularityDriver singularityDriver) {
    this.singularityDriver = singularityDriver;
  }

  @Override
  public void cacheOffer(long timestamp, Offer offer) {
    singularityDriver.declineOffer(offer.getId());
  }

  @Override
  public void rescindOffer(OfferID offerId) {
    // no-op
  }

  @Override
  public void useOffer(CachedOffer cachedOffer) {
    // no-op
  }

  @Override
  public List<CachedOffer> checkoutOffers() {
    return Collections.emptyList();
  }

  @Override
  public void returnOffer(CachedOffer cachedOffer) {
    // no-op
  }

  @Override
  public List<Offer> peekOffers() {
    return Collections.emptyList();
  }

  @Override
  public void disableOfferCache() {
    // no-op
  }

  @Override
  public void enableOfferCache() {
    // no-op
  }

}
