package com.buysloans.hub

import org.junit.Assert.*
import org.junit.Test

class PricingRegressionTest {
    private fun norm(s:String)=s.lowercase().replace(Regex("[^a-z0-9]+")," ").replace(Regex("\\s+")," ").trim()
    private fun isAccessory(title:String)=Regex("\\b(cable|adapter|charger|battery|keyboard|mouse|case|cover|stand|webcam|module|ssd|ram kit|memory|motherboard|heatsink|fan|screen|display|replacement|part|parts)\\b").containsMatchIn(norm(title))
    private fun maxBuy(sale:Double,gp:Double)=sale*(1-gp/100.0)

    @Test fun accessoryResultsAreRejected(){assertTrue(isAccessory("MSI Trident replacement motherboard part"));assertTrue(isAccessory("RTX 4070 graphics card fan replacement"));assertTrue(isAccessory("Laptop charger adapter"))}
    @Test fun completeDevicesAreNotBlanketAccessories(){assertFalse(isAccessory("MSI MPG Trident AS 14NUC7 gaming desktop PC"));assertFalse(isAccessory("Dell XPS 15 laptop i7 32GB"))}
    @Test fun gpTargetsMatchApprovedGrades(){assertEquals(700.0,maxBuy(1000.0,30.0),0.001);assertEquals(500.0,maxBuy(1000.0,50.0),0.001);assertEquals(300.0,maxBuy(1000.0,70.0),0.001);assertEquals(700.0,maxBuy(1000.0,30.0),0.001)}
    @Test fun componentTermsRemainDistinct(){assertTrue(norm("RTX 4070 graphics card").contains("rtx 4070"));assertTrue(norm("13700F processor").contains("13700f"));assertTrue(norm("32GB DDR5 RAM").contains("32gb"));assertTrue(norm("1TB NVMe SSD").contains("1tb"))}
}
