(function attachMorleyAssessmentClient(root){
  'use strict';
  const VALID_DIAGNOSTIC_STATUSES=new Set(['pass','fail','unknown','not_tested']);
  const VALID_SEVERITIES=new Set(['low','medium','high','critical']);
  const VALID_CHECKPOINTS=new Set(['capture_started','front_captured','rear_captured','analysis_started','analysis_failed','review_ready','review_completed','pricing_started','pricing_ready','repair_decision_ready','staff_confirmed','stock_prepared','cancelled','completed']);
  function result(ok,code,data=null,recoverable=true){return Object.freeze({ok,code,data,recoverable})}
  function text(value){return typeof value==='string'?value.trim():''}
  function numberOrNull(value){const n=Number(value);return Number.isFinite(n)?n:null}
  function clamp(value,min,max){const n=numberOrNull(value);return n===null?min:Math.min(max,Math.max(min,n))}
  function cleanMetadata(input){
    const source=input&&typeof input==='object'&&!Array.isArray(input)?input:{};
    const blocked=/^(imei|imei\d*|serial|serialnumber|serial_number|rawidentifier|raw_identifier|deviceidentifier|device_identifier|access[_-]?token|refresh[_-]?token|service[_-]?role|password|secret)$/i;
    return Object.fromEntries(Object.entries(source).filter(([key])=>!blocked.test(String(key))));
  }
  async function session(){
    const sb=root.sb;
    if(!sb?.auth?.getSession)return null;
    const response=await sb.auth.getSession();
    return response?.data?.session||null;
  }
  async function requireSession(){const current=await session();return current?.user?.id?current:null}
  async function createAssessment(input={}){
    const current=await requireSession();if(!current)return result(false,'auth_required',null,true);
    const payload={created_by:current.user.id,state:text(input.state)||'draft'};
    if(text(input.catalogueRef))payload.catalogue_ref=text(input.catalogueRef);
    if(text(input.stockRef))payload.stock_ref=text(input.stockRef);
    if(text(input.resolvedModel))payload.resolved_model=text(input.resolvedModel);
    if(text(input.source))payload.source=text(input.source);
    const checkpoint=text(input.checkpoint).toLowerCase();
    if(checkpoint){if(!VALID_CHECKPOINTS.has(checkpoint))return result(false,'invalid_checkpoint',null,true);payload.checkpoint=checkpoint;payload.last_checkpoint_at=new Date().toISOString()}
    const storage=numberOrNull(input.resolvedStorageGb);if(storage!==null&&storage>=0)payload.resolved_storage_gb=Math.round(storage);
    const confidence=numberOrNull(input.identityConfidence);if(confidence!==null)payload.identity_confidence=clamp(confidence,0,1);
    const {data,error}=await root.sb.from('device_assessments').insert(payload).select('id').single();
    if(error)return result(false,'assessment_create_failed',{message:error.message||'Assessment could not be created.'},true);
    return result(true,'created',{id:data?.id||null},false);
  }
  async function checkpointAssessment(assessmentId,input={}){
    const current=await requireSession();if(!current)return result(false,'auth_required',null,true);
    const id=text(assessmentId);if(!id)return result(false,'assessment_required',null,true);
    const checkpoint=text(input.checkpoint).toLowerCase();if(!VALID_CHECKPOINTS.has(checkpoint))return result(false,'invalid_checkpoint',null,true);
    const now=new Date().toISOString();
    const update={checkpoint,last_checkpoint_at:now,checkpoint_metadata:cleanMetadata(input.metadata)};
    const errorCode=text(input.errorCode);if(errorCode)update.last_error_code=errorCode;else update.last_error_code=null;
    if(checkpoint==='cancelled')update.cancelled_at=now;
    if(checkpoint==='completed')update.completed_at=now;
    const {error}=await root.sb.from('device_assessments').update(update).eq('id',id);
    if(error)return result(false,'checkpoint_write_failed',{message:error.message||'Scan checkpoint could not be saved.'},true);
    return result(true,'checkpoint_saved',{id,checkpoint},false);
  }
  async function listScanHistory(input={}){
    const current=await requireSession();if(!current)return result(false,'auth_required',null,true);
    const requested=Math.floor(numberOrNull(input.limit)??50),limit=Math.min(100,Math.max(1,requested));
    const query=root.sb.from('device_assessments')
      .select('id,state,source,checkpoint,resolved_model,resolved_storage_gb,identity_confidence,last_error_code,last_checkpoint_at,cancelled_at,completed_at,created_at,updated_at')
      .eq('source','device_lens')
      .order('created_at',{ascending:false})
      .limit(limit);
    const {data,error}=await query;
    if(error)return result(false,'scan_history_read_failed',{message:error.message||'Scan history could not be loaded.'},true);
    return result(true,'loaded',Array.isArray(data)?data:[],false);
  }
  async function getScanHistory(assessmentId){
    const current=await requireSession();if(!current)return result(false,'auth_required',null,true);
    const id=text(assessmentId);if(!id)return result(false,'assessment_required',null,true);
    const {data,error}=await root.sb.from('device_assessments')
      .select('id,state,source,checkpoint,resolved_model,resolved_storage_gb,identity_confidence,last_error_code,last_checkpoint_at,cancelled_at,completed_at,created_at,updated_at')
      .eq('id',id).single();
    if(error)return result(false,'scan_history_read_failed',{message:error.message||'Scan could not be loaded.'},true);
    return result(true,'loaded',data||null,false);
  }
  async function addEvidence(assessmentId,input={}){
    const current=await requireSession();if(!current)return result(false,'auth_required',null,true);
    const id=text(assessmentId);if(!id)return result(false,'assessment_required',null,true);
    const payload={assessment_id:id,evidence_type:text(input.type)||'unknown',source:text(input.source)||'unknown',confidence:clamp(input.confidence,0,1),verified:input.verified===true,verification_status:input.verified===true?'verified':'pending',capture_quality:cleanMetadata(input.captureQuality),metadata:cleanMetadata(input.metadata),created_by:current.user.id};
    if(text(input.storagePath))payload.storage_path=text(input.storagePath);
    if(text(input.protectedIdentifierRef))payload.protected_identifier_ref=text(input.protectedIdentifierRef);
    const {error}=await root.sb.from('assessment_evidence').insert(payload);
    if(error)return result(false,'evidence_write_failed',{message:error.message||'Evidence could not be recorded.'},true);
    return result(true,'recorded',null,false);
  }
  async function recordDiagnostic(assessmentId,input={}){
    const current=await requireSession();if(!current)return result(false,'auth_required',null,true);
    const status=text(input.status).toLowerCase();if(!VALID_DIAGNOSTIC_STATUSES.has(status))return result(false,'invalid_diagnostic_status',null,true);
    const severity=text(input.severity).toLowerCase();
    const payload={assessment_id:text(assessmentId),test_type:text(input.test)||'unknown',status,severity:VALID_SEVERITIES.has(severity)?severity:'low',measurement:cleanMetadata(input.measurement),confidence:clamp(input.confidence,0,1),source:text(input.source)||'guided',created_by:current.user.id};
    const {error}=await root.sb.from('diagnostic_results').insert(payload);
    if(error)return result(false,'diagnostic_write_failed',{message:error.message||'Diagnostic result could not be recorded.'},true);
    return result(true,'recorded',null,false);
  }
  function requestProposal(input={}){
    const core=root.MorleyAssessmentCore;if(!core?.buildAssessmentProposal)return result(false,'core_unavailable',null,true);
    return result(true,'proposed',core.buildAssessmentProposal(input),false);
  }
  async function confirmCommercialDecision(assessmentId,input={}){
    const current=await requireSession();if(!current)return result(false,'auth_required',null,true);
    if(input.explicitStaffConfirmation!==true)return result(false,'staff_confirmation_required',null,true);
    const update={};const now=new Date().toISOString();
    if(text(input.finalGrade)){update.final_grade=text(input.finalGrade);update.grade_confirmed_by=current.user.id;update.grade_confirmed_at=now}
    const cents=numberOrNull(input.finalBuyPriceCents);if(cents!==null&&cents>=0){update.final_buy_price_cents=Math.round(cents);update.buy_price_confirmed_by=current.user.id;update.buy_price_confirmed_at=now}
    if(text(input.repairDecision)){update.repair_decision=text(input.repairDecision);update.repair_decision_confirmed_by=current.user.id;update.repair_decision_confirmed_at=now}
    if(!Object.keys(update).length)return result(false,'commercial_decision_required',null,true);
    const {error}=await root.sb.from('device_assessments').update(update).eq('id',text(assessmentId));
    if(error)return result(false,'commercial_confirmation_failed',{message:error.message||'Commercial confirmation could not be saved.'},true);
    return result(true,'confirmed',null,false);
  }
  function prepareStockPayload(input={}){
    const core=root.MorleyAssessmentCore;if(!core?.canPrepareStock)return result(false,'core_unavailable',null,true);
    if(!core.canPrepareStock(input))return result(false,'stock_preparation_blocked',null,true);
    const payload=Object.freeze({model:text(input.model),modelNumber:text(input.modelNumber),storage:text(input.storage),grade:text(input.grade),buyPrice:numberOrNull(input.buyPrice),targetResale:numberOrNull(input.targetResale),repairDecision:text(input.repairDecision),description:text(input.description)});
    return Object.freeze({ok:true,code:'ready_for_staff_publish',payload,recoverable:false,requiresStaffPublish:true});
  }
  root.MorleyAssessmentClient=Object.freeze({version:'1.1.0',createAssessment,checkpointAssessment,listScanHistory,getScanHistory,addEvidence,recordDiagnostic,requestProposal,confirmCommercialDecision,prepareStockPayload});
})(typeof globalThis!=='undefined'?globalThis:window);
