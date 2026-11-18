export default function DataTable({ columns, rows, emptyText = 'Không có dữ liệu' }) {
  return <div style={{overflowX:'auto',background:'#fff',borderRadius:12,border:'1px solid #e5e7eb'}}>
    <table style={{width:'100%',borderCollapse:'collapse'}}><thead><tr>{columns.map(c=><th key={c.key} style={{textAlign:'left',padding:12,background:'#f8fafc',borderBottom:'1px solid #e5e7eb'}}>{c.label}</th>)}</tr></thead>
      <tbody>{rows.length ? rows.map((r,i)=><tr key={r.id||i}>{columns.map(c=><td key={c.key} style={{padding:12,borderBottom:'1px solid #f1f5f9'}}>{c.render?c.render(r):r[c.key]}</td>)}</tr>) : <tr><td colSpan={columns.length} style={{padding:20,textAlign:'center',color:'#64748b'}}>{emptyText}</td></tr>}</tbody>
    </table></div>
}
