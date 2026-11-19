import { useEffect, useState } from 'react'
import { api, formatDate, formatMoney } from '../api/client.js'
import DataTable from '../components/DataTable.jsx'
export default function TicketsPage(){
 const [rows,setRows]=useState([]),[status,setStatus]=useState(''),[readerId,setReaderId]=useState(''),[error,setError]=useState('')
 async function load(){try{setError('');const q=new URLSearchParams();if(status)q.set('status',status);if(readerId)q.set('readerId',readerId);setRows(await api.get('/api/tickets'+(q.size?'?'+q:''))||[])}catch(e){setError(e.message)}}
 useEffect(()=>{load()},[status,readerId])
 const columns=[{key:'id',label:'Mã phiếu'},{key:'readerId',label:'Mã bạn đọc'},{key:'borrowDate',label:'Ngày mượn',render:r=>formatDate(r.borrowDate)},{key:'dueDate',label:'Hạn trả',render:r=>formatDate(r.dueDate)},{key:'daysBorrowed',label:'Số ngày mượn'},{key:'overdue',label:'Quá hạn',render:r=>r.overdue?'Có':'Không'},{key:'status',label:'Trạng thái',render:r=>r.status==='RETURNED'?'Đã trả':'Đang mượn'},{key:'fineAmount',label:'Tiền phạt',render:r=>formatMoney(r.fineAmount)}]
 return <section className="page"><h1>Phiếu mượn</h1><div style={{display:'flex',gap:12,margin:'16px 0',flexWrap:'wrap'}}><select value={status} onChange={e=>setStatus(e.target.value)}><option value="">Tất cả trạng thái</option><option value="BORROWING">Đang mượn</option><option value="OVERDUE">Quá hạn</option><option value="RETURNED">Đã trả</option></select><input placeholder="Mã bạn đọc" value={readerId} onChange={e=>setReaderId(e.target.value)}/><button onClick={load}>Tải lại</button></div>{error&&<p style={{color:'crimson'}}>{error}</p>}<DataTable columns={columns} rows={rows}/></section>
}
