package com.niuma.driver.data

import androidx.room.*
import com.niuma.driver.domain.balance.*
import com.niuma.driver.domain.decision.*
import com.niuma.driver.domain.lifetime.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Entity(tableName="balance_items") data class BalanceEntity(@PrimaryKey(autoGenerate=true) val id:Long=0,val name:String,val price:String,val category:String?,val remark:String,val status:String,val isPinned:Boolean,val usageMonths:Int?,val frequencyType:String?,val frequencyValue:String?,val oneTimeExtra:String,val monthlyHolding:String,val annualHolding:String,val createdAt:String,val updatedAt:String,val purchasedAt:String?) {
    fun domain()=BalanceItem(id,name,BigDecimal(price),category?.let(BalanceCategory::valueOf),remark,BalanceStatus.valueOf(status),isPinned,usageMonths,frequencyType?.let(UsageFrequencyType::valueOf),frequencyValue?.let(::BigDecimal),BigDecimal(oneTimeExtra),BigDecimal(monthlyHolding),BigDecimal(annualHolding),LocalDateTime.parse(createdAt),LocalDateTime.parse(updatedAt),purchasedAt?.let(LocalDateTime::parse))
    companion object { fun of(x:BalanceItem)=BalanceEntity(x.id,x.name,x.price.toPlainString(),x.category?.name,x.remark,x.status.name,x.isPinned,x.expectedUsagePeriodMonths,x.usageFrequencyType?.name,x.usageFrequencyValue?.toPlainString(),x.oneTimeExtraCost.toPlainString(),x.monthlyHoldingCost.toPlainString(),x.annualHoldingCost.toPlainString(),x.createdAt.toString(),x.updatedAt.toString(),x.purchasedAt?.toString()) }
}
@Entity(tableName="life_nodes") data class LifeNodeEntity(@PrimaryKey(autoGenerate=true) val id:Long=0,val title:String,val type:String,val targetAge:Int?,val targetDate:String?,val createdAt:String) {
    fun domain()=LifeNode(id,title,LifeNodeType.valueOf(type),targetAge,targetDate?.let(LocalDate::parse),LocalDateTime.parse(createdAt)); companion object { fun of(x:LifeNode)=LifeNodeEntity(x.id,x.title,x.type.name,x.targetAge,x.targetDate?.toString(),x.createdAt.toString()) }
}
@Entity(tableName="decisions") data class DecisionEntity(@PrimaryKey(autoGenerate=true) val id:Long=0,val title:String,val category:String?,val background:String,val initialPreference:String,val customPreference:String,val confidence:Int?,val mainReason:String,val biggestRisk:String,val expectedResult:String,val status:String,val finalDecision:String,val createdAt:String,val decidedAt:String?,val reviewDate:String?) {
    fun domain()=Decision(id,title,category?.let(DecisionCategory::valueOf),background,DecisionPreference.valueOf(initialPreference),customPreference,confidence,mainReason,biggestRisk,expectedResult,DecisionStatus.valueOf(status),finalDecision,LocalDateTime.parse(createdAt),decidedAt?.let(LocalDateTime::parse),reviewDate?.let(LocalDate::parse)); companion object { fun of(x:Decision)=DecisionEntity(x.id,x.title,x.category?.name,x.background,x.initialPreference.name,x.customPreference,x.confidence,x.mainReason,x.biggestRisk,x.expectedResult,x.status.name,x.finalDecision,x.createdAt.toString(),x.decidedAt?.toString(),x.reviewDate?.toString()) }
}
@Entity(tableName="decision_reviews") data class ReviewEntity(@PrimaryKey(autoGenerate=true) val id:Long=0,val decisionId:Long,val resultRating:String,val wouldChooseAgain:String,val satisfactionScore:Int?,val correctJudgment:String,val wrongJudgment:String,val reflection:String,val reviewedAt:String) {
    fun domain()=DecisionReview(id,decisionId,ResultRating.valueOf(resultRating),ChooseAgain.valueOf(wouldChooseAgain),satisfactionScore,correctJudgment,wrongJudgment,reflection,LocalDateTime.parse(reviewedAt)); companion object { fun of(x:DecisionReview)=ReviewEntity(x.id,x.decisionId,x.resultRating.name,x.wouldChooseAgain.name,x.satisfactionScore,x.correctJudgment,x.wrongJudgment,x.reflection,x.reviewedAt.toString()) }
}
@Dao interface LifeDao {
    @Query("SELECT * FROM balance_items ORDER BY isPinned DESC, updatedAt DESC") fun balances():Flow<List<BalanceEntity>>
    @Query("SELECT * FROM life_nodes ORDER BY createdAt") fun nodes():Flow<List<LifeNodeEntity>>
    @Query("SELECT * FROM decisions ORDER BY createdAt DESC") fun decisions():Flow<List<DecisionEntity>>
    @Query("SELECT * FROM decision_reviews ORDER BY reviewedAt DESC") fun reviews():Flow<List<ReviewEntity>>
    @Upsert suspend fun saveBalance(x:BalanceEntity):Long
    @Query("UPDATE balance_items SET isPinned=0") suspend fun clearPins()
    @Query("UPDATE balance_items SET isPinned=1 WHERE id=:id") suspend fun pin(id:Long)
    @Query("DELETE FROM balance_items WHERE id=:id") suspend fun deleteBalance(id:Long)
    @Upsert suspend fun saveNode(x:LifeNodeEntity):Long
    @Query("DELETE FROM life_nodes WHERE id=:id") suspend fun deleteNode(id:Long)
    @Upsert suspend fun saveDecision(x:DecisionEntity):Long
    @Query("DELETE FROM decisions WHERE id=:id") suspend fun deleteDecision(id:Long)
    @Query("DELETE FROM decision_reviews WHERE decisionId=:id") suspend fun deleteReviews(id:Long)
    @Upsert suspend fun saveReview(x:ReviewEntity):Long
    @Transaction suspend fun setPinned(id:Long?){clearPins();if(id!=null)pin(id)}
    @Transaction suspend fun removeDecision(id:Long){deleteReviews(id);deleteDecision(id)}
}
@Database(entities=[BalanceEntity::class,LifeNodeEntity::class,DecisionEntity::class,ReviewEntity::class],version=1,exportSchema=true)
abstract class LifeDatabase:RoomDatabase(){ abstract fun dao():LifeDao }
class LifeRepository(db:LifeDatabase){ private val dao=db.dao(); val balances=dao.balances().map{it.map(BalanceEntity::domain)}; val nodes=dao.nodes().map{it.map(LifeNodeEntity::domain)}; val decisions=dao.decisions().map{it.map(DecisionEntity::domain)}; val reviews=dao.reviews().map{it.map(ReviewEntity::domain)}
    suspend fun save(x:BalanceItem)=dao.saveBalance(BalanceEntity.of(x)); suspend fun pin(id:Long?)=dao.setPinned(id); suspend fun deleteBalance(id:Long)=dao.deleteBalance(id)
    suspend fun save(x:LifeNode)=dao.saveNode(LifeNodeEntity.of(x)); suspend fun deleteNode(id:Long)=dao.deleteNode(id)
    suspend fun save(x:Decision)=dao.saveDecision(DecisionEntity.of(x)); suspend fun deleteDecision(id:Long)=dao.removeDecision(id); suspend fun save(x:DecisionReview)=dao.saveReview(ReviewEntity.of(x))
}
