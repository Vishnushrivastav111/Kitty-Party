import { NgClass } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DialogService } from '../../../shared/dialog.service';
import { Validators, formatDate, queryRows } from '../../../shared/format';
import { Feedback, FeedbackHistory } from '../../../model/models';
import { UserService } from '../../../service/user.service';
import { PagerComponent } from '../../../shared/pager.component';
@Component({selector:'app-feedback',standalone:true,imports:[FormsModule,PagerComponent,NgClass],templateUrl:'./feedback.component.html',styleUrl:'./feedback.component.scss'})
export class FeedbackComponent {
 private readonly vault=inject(UserService); private readonly dialog=inject(DialogService); readonly categories=['General','Bug','Feature','UI','Other']; readonly date=formatDate; readonly search=signal(''); readonly status=signal(''); readonly category=signal(''); readonly pageNo=signal(1); readonly editing=signal(false); readonly busy=signal(false); readonly errors=signal<Record<string,string>>({}); readonly historyOpen=signal(false); readonly history=signal<FeedbackHistory[]>([]); historyMeta=''; form:Feedback=blank();
 readonly page=computed(()=>queryRows(this.vault.feedback(),this.search(),['subject','message','category'],{status:this.status(),category:this.category()},this.pageNo(),5));
 badge(s:string){return s==='resolved'?'badge-success':s==='in-review'?'badge-warning':s==='closed'?'badge-neutral':'badge-info'}
 open(r:Feedback|null){this.form=r?{...r}:blank();this.errors.set({});this.editing.set(true)}
 validate(field:string){const e={...this.errors()};if(field==='subject')e.subject=Validators.required(this.form.subject,'Subject');if(field==='category')e.category=Validators.required(this.form.category,'Category');if(field==='message')e.message=Validators.required(this.form.message,'Message');this.errors.set(e)}
 async save(){const errors={subject:Validators.required(this.form.subject,'Subject'),category:Validators.required(this.form.category,'Category'),message:Validators.required(this.form.message,'Message')};this.errors.set(errors);if(Object.values(errors).some(Boolean))return;this.busy.set(true);try{const body={subject:this.form.subject.trim(),category:this.form.category,message:this.form.message.trim(),status:this.form.status||'open'};if(this.form.id)await this.vault.updateFeedback(this.form.id,body);else await this.vault.addFeedback(body);this.editing.set(false)}catch(e){await this.dialog.notice(e instanceof Error?e.message:'Could not save feedback')}finally{this.busy.set(false)}}
 async showHistory(r:Feedback){this.historyMeta=r.subject;this.history.set(await this.vault.history(r.id));this.historyOpen.set(true)}
}
function blank():Feedback{return{id:'',subject:'',category:'',message:'',status:'open'}}
