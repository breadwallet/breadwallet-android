(function(t){function e(e){for(var a,c,o=e[0],l=e[1],r=e[2],u=0,f=[];u<o.length;u++)c=o[u],i[c]&&f.push(i[c][0]),i[c]=0;for(a in l)Object.prototype.hasOwnProperty.call(l,a)&&(t[a]=l[a]);d&&d(e);while(f.length)f.shift()();return n.push.apply(n,r||[]),s()}function s(){for(var t,e=0;e<n.length;e++){for(var s=n[e],a=!0,o=1;o<s.length;o++){var l=s[o];0!==i[l]&&(a=!1)}a&&(n.splice(e--,1),t=c(c.s=s[0]))}return t}var a={},i={app:0},n=[];function c(e){if(a[e])return a[e].exports;var s=a[e]={i:e,l:!1,exports:{}};return t[e].call(s.exports,s,s.exports,c),s.l=!0,s.exports}c.m=t,c.c=a,c.d=function(t,e,s){c.o(t,e)||Object.defineProperty(t,e,{enumerable:!0,get:s})},c.r=function(t){"undefined"!==typeof Symbol&&Symbol.toStringTag&&Object.defineProperty(t,Symbol.toStringTag,{value:"Module"}),Object.defineProperty(t,"__esModule",{value:!0})},c.t=function(t,e){if(1&e&&(t=c(t)),8&e)return t;if(4&e&&"object"===typeof t&&t&&t.__esModule)return t;var s=Object.create(null);if(c.r(s),Object.defineProperty(s,"default",{enumerable:!0,value:t}),2&e&&"string"!=typeof t)for(var a in t)c.d(s,a,function(e){return t[e]}.bind(null,a));return s},c.n=function(t){var e=t&&t.__esModule?function(){return t["default"]}:function(){return t};return c.d(e,"a",e),e},c.o=function(t,e){return Object.prototype.hasOwnProperty.call(t,e)},c.p="";var o=window["webpackJsonp"]=window["webpackJsonp"]||[],l=o.push.bind(o);o.push=e,o=o.slice();for(var r=0;r<o.length;r++)e(o[r]);var d=l;n.push([0,"chunk-vendors"]),s()})({0:function(t,e,s){t.exports=s("56d7")},"199c":function(t,e){},"1ccc":function(t,e,s){"use strict";var a=s("1d88"),i=s.n(a);i.a},"1d88":function(t,e,s){},"1e90":function(t,e,s){"use strict";var a=s("af70"),i=s.n(a);i.a},"1f96":function(t,e,s){},"21bb":function(t,e,s){"use strict";var a=s("bcc9"),i=s.n(a);i.a},"23be":function(t,e,s){"use strict";var a=s("199c"),i=s.n(a);e["default"]=i.a},"2e75":function(t,e,s){},"3c7e":function(t,e,s){"use strict";var a=s("7baa"),i=s.n(a);i.a},"3dfd":function(t,e,s){"use strict";var a=s("56ac"),i=s("23be"),n=(s("5c0b"),s("2877")),c=Object(n["a"])(i["default"],a["a"],a["b"],!1,null,null,null);e["default"]=c.exports},4678:function(t,e,s){var a={"./af":"2bfb","./af.js":"2bfb","./ar":"8e73","./ar-dz":"a356","./ar-dz.js":"a356","./ar-kw":"423e","./ar-kw.js":"423e","./ar-ly":"1cfd","./ar-ly.js":"1cfd","./ar-ma":"0a84","./ar-ma.js":"0a84","./ar-sa":"8230","./ar-sa.js":"8230","./ar-tn":"6d83","./ar-tn.js":"6d83","./ar.js":"8e73","./az":"485c","./az.js":"485c","./be":"1fc1","./be.js":"1fc1","./bg":"84aa","./bg.js":"84aa","./bm":"a7fa","./bm.js":"a7fa","./bn":"9043","./bn.js":"9043","./bo":"d26a","./bo.js":"d26a","./br":"6887","./br.js":"6887","./bs":"2554","./bs.js":"2554","./ca":"d716","./ca.js":"d716","./cs":"3c0d","./cs.js":"3c0d","./cv":"03ec","./cv.js":"03ec","./cy":"9797","./cy.js":"9797","./da":"0f14","./da.js":"0f14","./de":"b469","./de-at":"b3eb","./de-at.js":"b3eb","./de-ch":"bb71","./de-ch.js":"bb71","./de.js":"b469","./dv":"598a","./dv.js":"598a","./el":"8d47","./el.js":"8d47","./en-SG":"cdab","./en-SG.js":"cdab","./en-au":"0e6b","./en-au.js":"0e6b","./en-ca":"3886","./en-ca.js":"3886","./en-gb":"39a6","./en-gb.js":"39a6","./en-ie":"e1d3","./en-ie.js":"e1d3","./en-il":"7333","./en-il.js":"7333","./en-nz":"6f50","./en-nz.js":"6f50","./eo":"65db","./eo.js":"65db","./es":"898b","./es-do":"0a3c","./es-do.js":"0a3c","./es-us":"55c9","./es-us.js":"55c9","./es.js":"898b","./et":"ec18","./et.js":"ec18","./eu":"0ff2","./eu.js":"0ff2","./fa":"8df4","./fa.js":"8df4","./fi":"81e9","./fi.js":"81e9","./fo":"0721","./fo.js":"0721","./fr":"9f26","./fr-ca":"d9f8","./fr-ca.js":"d9f8","./fr-ch":"0e49","./fr-ch.js":"0e49","./fr.js":"9f26","./fy":"7118","./fy.js":"7118","./ga":"5120","./ga.js":"5120","./gd":"f6b4","./gd.js":"f6b4","./gl":"8840","./gl.js":"8840","./gom-latn":"0caa","./gom-latn.js":"0caa","./gu":"e0c5","./gu.js":"e0c5","./he":"c7aa","./he.js":"c7aa","./hi":"dc4d","./hi.js":"dc4d","./hr":"4ba9","./hr.js":"4ba9","./hu":"5b14","./hu.js":"5b14","./hy-am":"d6b6","./hy-am.js":"d6b6","./id":"5038","./id.js":"5038","./is":"0558","./is.js":"0558","./it":"6e98","./it-ch":"6f12","./it-ch.js":"6f12","./it.js":"6e98","./ja":"079e","./ja.js":"079e","./jv":"b540","./jv.js":"b540","./ka":"201b","./ka.js":"201b","./kk":"6d79","./kk.js":"6d79","./km":"e81d","./km.js":"e81d","./kn":"3e92","./kn.js":"3e92","./ko":"22f8","./ko.js":"22f8","./ku":"2421","./ku.js":"2421","./ky":"9609","./ky.js":"9609","./lb":"440c","./lb.js":"440c","./lo":"b29d","./lo.js":"b29d","./lt":"26f9","./lt.js":"26f9","./lv":"b97c","./lv.js":"b97c","./me":"293c","./me.js":"293c","./mi":"688b","./mi.js":"688b","./mk":"6909","./mk.js":"6909","./ml":"02fb","./ml.js":"02fb","./mn":"958b","./mn.js":"958b","./mr":"39bd","./mr.js":"39bd","./ms":"ebe4","./ms-my":"6403","./ms-my.js":"6403","./ms.js":"ebe4","./mt":"1b45","./mt.js":"1b45","./my":"8689","./my.js":"8689","./nb":"6ce3","./nb.js":"6ce3","./ne":"3a39","./ne.js":"3a39","./nl":"facd","./nl-be":"db29","./nl-be.js":"db29","./nl.js":"facd","./nn":"b84c","./nn.js":"b84c","./pa-in":"f3ff","./pa-in.js":"f3ff","./pl":"8d57","./pl.js":"8d57","./pt":"f260","./pt-br":"d2d4","./pt-br.js":"d2d4","./pt.js":"f260","./ro":"972c","./ro.js":"972c","./ru":"957c","./ru.js":"957c","./sd":"6784","./sd.js":"6784","./se":"ffff","./se.js":"ffff","./si":"eda5","./si.js":"eda5","./sk":"7be6","./sk.js":"7be6","./sl":"8155","./sl.js":"8155","./sq":"c8f3","./sq.js":"c8f3","./sr":"cf1e","./sr-cyrl":"13e9","./sr-cyrl.js":"13e9","./sr.js":"cf1e","./ss":"52bd","./ss.js":"52bd","./sv":"5fbd","./sv.js":"5fbd","./sw":"74dc","./sw.js":"74dc","./ta":"3de5","./ta.js":"3de5","./te":"5cbb","./te.js":"5cbb","./tet":"576c","./tet.js":"576c","./tg":"3b1b","./tg.js":"3b1b","./th":"10e8","./th.js":"10e8","./tl-ph":"0f38","./tl-ph.js":"0f38","./tlh":"cf75","./tlh.js":"cf75","./tr":"0e81","./tr.js":"0e81","./tzl":"cf51","./tzl.js":"cf51","./tzm":"c109","./tzm-latn":"b53d","./tzm-latn.js":"b53d","./tzm.js":"c109","./ug-cn":"6117","./ug-cn.js":"6117","./uk":"ada2","./uk.js":"ada2","./ur":"5294","./ur.js":"5294","./uz":"2e8c","./uz-latn":"010e","./uz-latn.js":"010e","./uz.js":"2e8c","./vi":"2921","./vi.js":"2921","./x-pseudo":"fd7e","./x-pseudo.js":"fd7e","./yo":"7f33","./yo.js":"7f33","./zh-cn":"5c3a","./zh-cn.js":"5c3a","./zh-hk":"49ab","./zh-hk.js":"49ab","./zh-tw":"90ea","./zh-tw.js":"90ea"};function i(t){var e=n(t);return s(e)}function n(t){var e=a[t];if(!(e+1)){var s=new Error("Cannot find module '"+t+"'");throw s.code="MODULE_NOT_FOUND",s}return e}i.keys=function(){return Object.keys(a)},i.resolve=n,t.exports=i,i.id="4678"},5628:function(t,e,s){"use strict";var a=s("2e75"),i=s.n(a);i.a},"56ac":function(t,e,s){"use strict";var a=function(){var t=this,e=t.$createElement,s=t._self._c||e;return s("div",{attrs:{id:"app"}},[s("router-view")],1)},i=[];s.d(e,"a",function(){return a}),s.d(e,"b",function(){return i})},"56d7":function(t,e,s){"use strict";s.r(e);s("cadf"),s("551c"),s("f751"),s("097d");var a=s("2b0e"),i=s("3dfd"),n=s("8c4f"),c=function(){var t=this,e=t.$createElement,s=t._self._c||e;return s("div",{staticClass:"p-app"},[s("mt-tab-container",{attrs:{swipeable:!1},model:{value:t.active,callback:function(e){t.active=e},expression:"active"}},[s("mt-tab-container-item",{attrs:{id:"tab1"}},[s("Nodes")],1),s("mt-tab-container-item",{attrs:{id:"tab2"}},[s("MyFav")],1),s("mt-tab-container-item",{attrs:{id:"tab3"}},[s("MyVotes")],1)],1),t.tab_show?s("mt-tabbar",{model:{value:t.active,callback:function(e){t.active=e},expression:"active"}},[s("mt-tab-item",{attrs:{id:"tab1"}},[s("i",{staticClass:"iconfont icon-nodes"}),s("p",[t._v(t._s(t.$t("NODES")))])]),s("mt-tab-item",{attrs:{id:"tab2"}},[s("i",{staticClass:"iconfont icon-fav"}),s("p",[t._v(t._s(t.$t("FAVORITES")))])]),s("mt-tab-item",{attrs:{id:"tab3"}},[s("i",{staticClass:"iconfont icon-vote"}),s("p",[t._v(t._s(t.$t("MY_VOTES")))])])],1):t._e(),s("router-view")],1)},o=[],l=function(){var t=this,e=t.$createElement,s=t._self._c||e;return s("div",{staticClass:"p-nodes kg-page"},[s("div",{staticClass:"kg-body kg-tab"},[s("mt-search",{staticStyle:{height:"auto"},attrs:{value:t.value,"cancel-text":"Cancel",placeholder:"serach..."},on:{"update:value":function(e){t.value=e}}}),s("div",{staticClass:"c-filter"},[s("div",{staticClass:"c-e",class:1==t.filter_active?"active":"",on:{click:function(e){return t.clickFilter(1)}}},[s("p",[t._v(t._s(t.$t("RANK")))])]),s("div",{staticClass:"c-e",class:2==t.filter_active?"active":"",on:{click:function(e){return t.clickFilter(2)}}},[s("p",[t._v(t._s(t.$t("FAV")))])]),s("div",{staticClass:"c-e",class:3==t.filter_active?"active":"",on:{click:function(e){return t.clickFilter(3)}}},[s("p",[t._v(t._s(t.$t("LATEST")))])]),s("div",{staticClass:"c-e",class:4==t.filter_active?"active":"",on:{click:function(e){return t.clickFilter(4)}}},[s("p",[t._v("A-Z")])])]),s("div",{staticClass:"c-list"},t._l(t.list,function(e,a){return s("VotingListItem",{key:a,attrs:{status:t.vote_status,clickFn:t.clickItem,item:e,index:a+1}})}),1)],1),s("div",{staticClass:"x-btn",on:{click:function(e){return t.toggleListStatus()}}},[s("div",{staticClass:"kg-png",attrs:{slot:"icon"},slot:"icon"})]),s("div",{staticClass:"v-btn"},[s("mt-button",{staticClass:"cb",attrs:{disabled:t.select.n<1,size:"large",type:"primary"},on:{click:function(e){return t.clickVoteBtn()}}},[t._v("\n      Vote "),s("span",{staticStyle:{"font-size":"12px"}},[t._v(t._s("("+t.select.n+"/"+t.select.t+")"))])])],1)])},r=[],d=s("76a0"),u=s.n(d),f=s("2ef0"),_=s.n(f),p=s("c1df"),v=s.n(p),m=s("911a"),b=s.n(m),g={_:_.a,moment:v.a,loading:function(){var t=arguments.length>0&&void 0!==arguments[0]&&arguments[0];t?d["Indicator"].open({text:"Loading...",spinnerType:"fading-circle"}):d["Indicator"].close()},request:function(){arguments.length>0&&void 0!==arguments[0]&&arguments[0];var t=arguments.length>1&&void 0!==arguments[1]?arguments[1]:{};return new Promise(function(e){_.a.delay(function(){e(t)},1e3)})},register:b.a.subscribe,publish:b.a.publish,toastSuccess:function(t){Object(d["Toast"])({message:t,className:"kg-toast",iconClass:"fa fa-check"})},toastInfo:function(t){Object(d["Toast"])({message:t,className:"kg-toast",position:"bottom",duration:3e3})}},h=function(){var t=this,e=t.$createElement,a=t._self._c||e;return a("div",{staticClass:"cm-VotingListItem",class:{"s-vote":"vote"===t.status},on:{click:function(e){return t.clickItem()}}},[a("div",{staticClass:"c-checkbox",class:{selected:!!t.item.selected}},[a("div",{staticClass:"tc"})]),a("div",{staticClass:"c-icon"},[a("img",{attrs:{src:s("b640")}}),a("i",{staticClass:"c-status",class:{s1:"online"===t.item.network_status,s2:"offline"===t.item.network_status}})]),a("div",{staticClass:"c-bdy"},[a("p",{staticClass:"p p1"},[t._v(t._s(t.item.name))]),a("p",{staticClass:"p p2"},[t._v(t._s(t.index))]),a("i",{staticClass:"kg-png p p3"}),a("i",{staticClass:"kg-png p p4"}),a("p",{staticClass:"p p5"},[t._v(t._s(t.item.location))]),a("p",{staticClass:"p p6"},[t._v("+"+t._s(t.item.reward)+" ELA/year")]),a("i",{staticClass:"fa fa-star p p7",class:t.item.fav?"active":""}),a("p",{staticClass:"p p8"},[t._v(t._s(t.item.votes)+" votes")]),a("p",{staticClass:"p p9"},[t._v(t._s(t.item.percentage)+"%")]),a("div",{staticClass:"c-per",style:{width:t.item.percentage+"%"}})])])},k=[],j=(s("c5f6"),{props:{item:null,index:Number,status:null,clickFn:null},mounted:function(){},methods:{clickItem:function(){this.clickFn.call(null,this.item)}}}),y=j,C=(s("1ccc"),s("2877")),S=Object(C["a"])(y,h,k,!1,null,null,null),E=S.exports,w={components:{VotingListItem:E},data:function(){return{value:"",filter_active:1,vote_status:"list",select:{n:0,t:0}}},methods:{clickFilter:function(t){this.filter_active=t,this.$store.dispatch("set_node_list",{filter:t})},clickItem:function(t){"list"===this.vote_status?this.$router.push("/node_detail/"+t.id):(t.selected=!t.selected,this.processSelectNumber())},toggleListStatus:function(){this.vote_status="list"===this.vote_status?"vote":"list",g.publish("home-tab","list"===this.vote_status),"vote"===this.vote_status&&this.processSelectNumber()},processSelectNumber:function(){var t=g._.size(this.list),e=g._.size(g._.filter(this.list,function(t){return t.selected}));this.select={t:t,n:e}},clickVoteBtn:function(){alert("click vote button")}},computed:{list:function(){return this.$store.state.node_list?(g.loading(!1),this.$store.state.node_list):(g.loading(!0),[])}},mounted:function(){this.$store.dispatch("set_node_list",{})}},x=w,O=(s("ec0c"),Object(C["a"])(x,l,r,!1,null,null,null)),V=O.exports,L=function(){var t=this,e=t.$createElement,s=t._self._c||e;return s("div",{staticClass:"kg-page p-myVotes"},[s("div",{staticClass:"kg-body kg-tab"},[t.info?s("div",{staticClass:"kg-gap",staticStyle:{background:"#f9f9f9",padding:"12px 15px"}},[s("div",{staticClass:"c-rule"},[s("p",{staticClass:"t1"},[t._v(t._s(t.$t("VOTING_POWER_USED"))+"/"+t._s(t.$t("TOTAL"))+" (ELA)")]),s("p",{staticClass:"t2"},[t._v(t._s(t.info.vp_used)+"/"+t._s(t.info.vp_total))]),s("button",{staticClass:"t3",on:{click:function(e){return t.showPopUp()}}},[s("i",{staticClass:"fa fa-question-circle-o"}),t._v("\n          "+t._s(t.$t("RULE"))+"\n        ")])])]):t._e(),s("mt-navbar",{staticStyle:{"margin-top":"12px"},model:{value:t.selected,callback:function(e){t.selected=e},expression:"selected"}},[s("mt-tab-item",{attrs:{id:"all"}},[t._v(t._s(t.$t("ALL")))]),s("mt-tab-item",{attrs:{id:"success"}},[t._v(t._s(t.$t("SUCCESS")))]),s("mt-tab-item",{attrs:{id:"failure"}},[t._v(t._s(t.$t("FAILURE")))])],1),s("mt-tab-container",{model:{value:t.selected,callback:function(e){t.selected=e},expression:"selected"}},[s("mt-tab-container-item",{attrs:{id:"all"}},t._l(t.list,function(t,e){return s("MyVoteBaseList",{key:e,attrs:{data:t}})}),1),s("mt-tab-container-item",{attrs:{id:"success"}},t._l(4,function(t){return s("mt-cell",{attrs:{title:"测试 "+t}})}),1),s("mt-tab-container-item",{attrs:{id:"failure"}},t._l(6,function(t){return s("mt-cell",{attrs:{title:"选项 "+t}})}),1)],1)],1),s("mt-popup",{attrs:{"popup-transition":"popup-fade"},model:{value:t.popupVisible,callback:function(e){t.popupVisible=e},expression:"popupVisible"}},[s("div",{staticClass:"c-popup"},[s("div",{staticClass:"c-header"},[t._v(t._s(t.$t("VOTING_RULE")))]),s("div",{staticClass:"c-body"},[s("ul",[s("li",[t._v(t._s(t.$t("rule_msg.1"))+"\n          ")]),s("li",[t._v(t._s(t.$t("rule_msg.2")))])]),s("p",{staticClass:"p1"},[s("b",[t._v(t._s(t.$t("MORE_DETAIL")))])]),s("p",{staticClass:"p2"},[s("a",{attrs:{href:"/"}},[t._v("https://news.elastos.org/elastos-dpos-supernode-election-process/")])]),s("p",{staticClass:"p3"},[s("mt-button",{attrs:{size:"large",type:"primary"},on:{click:function(e){t.popupVisible=!1}}},[t._v("OK")])],1)])])])],1)},T=[],$=function(){var t=this,e=t.$createElement,s=t._self._c||e;return s("mt-cell",{staticClass:"cm-MyVoteBaseList",attrs:{to:"/vote_detail/"+t.data.id,title:t.data.number+"ELA","is-link":"",label:t.data.time}},[s("span",{staticClass:"t1"},[t._v(t._s(t.data.node)+" nodes")]),s("span",{staticClass:"t2"},[t._v(t._s(t.data.status))])])},A=[],I={props:{data:null}},N=I,z=(s("7938"),Object(C["a"])(N,$,A,!1,null,null,null)),R=z.exports,D={components:{MyVoteBaseList:R},data:function(){return{popupVisible:!1,selected:"all"}},methods:{showPopUp:function(){this.popupVisible=!0}},computed:{info:function(){return this.$store.state.me_info?(g.loading(!1),this.$store.state.me_info):(g.loading(!0),null)},list:function(){return g._.map(this.$store.state.my_votes_list,function(t){return t.time=g.moment(t.time).format("YYYY-MM-DD hh:mm"),t})}},mounted:function(){this.$store.dispatch("set_me_info",{}),this.$store.dispatch("set_my_votes_list",{})}},F=D,U=(s("1e90"),Object(C["a"])(F,L,T,!1,null,null,null)),M=U.exports,B=function(){var t=this,e=t.$createElement,s=t._self._c||e;return s("div",{staticClass:"p-myFav kg-page"},[s("div",{staticClass:"kg-body kg-tab"},[s("div",{staticClass:"c-list"},t._l(t.list,function(e,a){return s("VotingListItem",{key:a,attrs:{status:t.vote_status,clickFn:t.clickItem,item:e,index:a+1}})}),1)])])},W=[],P={data:function(){return{vote_status:"list"}},components:{VotingListItem:E},computed:{list:function(){return this.$store.state.my_fav_list?(g.loading(!1),this.$store.state.my_fav_list):(g.loading(!0),[])}},mounted:function(){this.$store.dispatch("set_my_fav_list",{})},methods:{clickItem:function(t){this.$router.push("/node_detail/"+t.id)}}},Y=P,q=(s("e47c"),Object(C["a"])(Y,B,W,!1,null,null,null)),G=q.exports,K={name:"home",data:function(){return{tab_show:!0,active:""}},computed:{},watch:{active:function(t,e){console.log(t,e),this.$store.commit("set_tab",t)}},components:{Nodes:V,MyVotes:M,MyFav:G},created:function(){var t=this;g.register("home-tab",function(e,s){t.tab_show=!!s})},mounted:function(){this.active=this.$store.state.current_tab}},J=K,Z=(s("21bb"),Object(C["a"])(J,c,o,!1,null,null,null)),H=Z.exports,Q=function(){var t=this,e=t.$createElement,a=t._self._c||e;return a("div",{staticClass:"kg-page p-NodeDetail"},[a("div",{staticClass:"kg-body"},[t.node_detail?a("div",{staticStyle:{background:"#fff",padding:"0 15px"}},[a("div",{staticClass:"c-head"},[a("img",{attrs:{src:s("b640")}}),a("span",[t._v(t._s(t.node_detail.name))]),a("i",{staticClass:"fa fa-star",class:{active:t.node_detail.fav}})]),a("div",{staticClass:"c-item"},[a("div",{staticClass:"c-icon kg-png",staticStyle:{"background-position":"-45px -141px"}}),a("p",{staticClass:"p1"},[t._v(t._s(t.$t("STATUS")))]),a("p",{staticClass:"p2"},[t._v(t._s(t.node_detail.status))])]),a("div",{staticClass:"c-item"},[a("div",{staticClass:"c-icon kg-png",staticStyle:{"background-position":"-70px -141px"}}),a("p",{staticClass:"p1"},[t._v(t._s(t.$t("RANK")))]),a("p",{staticClass:"p2"},[t._v(t._s(t.node_detail.rank))])]),a("div",{staticClass:"c-item"},[a("div",{staticClass:"c-icon kg-png",staticStyle:{"background-position":"-97px -141px"}}),a("p",{staticClass:"p1"},[t._v(t._s(t.$t("VOTES"))+" %")]),a("p",{staticClass:"p2"},[t._v(t._s(t.node_detail.percentage)+"%")])]),a("div",{staticClass:"c-item"},[a("div",{staticClass:"c-icon kg-png",staticStyle:{"background-position":"-123px -140px",height:"16px"}}),a("p",{staticClass:"p1"},[t._v(t._s(t.$t("LOCATION")))]),a("p",{staticClass:"p2"},[t._v(t._s(t.node_detail.location))])]),a("div",{staticClass:"c-item"},[a("div",{staticClass:"c-icon kg-png",staticStyle:{"background-position":"-149px -140px",width:"16px",height:"16px"}}),a("p",{staticClass:"p1"},[t._v("URL")]),a("p",{staticClass:"p2"},[t._v(t._s(t.node_detail.url))])]),a("div",{staticClass:"c-item"},[a("div",{staticClass:"c-icon kg-png",staticStyle:{"background-position":"-175px -140px",height:"16px"}}),a("p",{staticClass:"p1"},[t._v("Public key")]),a("p",{staticClass:"p2"},[a("span",{attrs:{id:"id_copy_text"}},[t._v(t._s(t.node_detail.public_key))]),a("i",{staticClass:"c-copy kg-png btn",staticStyle:{"background-position":"-228px -142px"},attrs:{"data-clipboard-text":t.node_detail.public_key}})])])]):t._e(),t.node_detail?a("div",{staticStyle:{background:"#fff",padding:"0 15px","margin-top":"10px","padding-bottom":"60px"}},t._l(t.node_detail.votes_gap,function(e,s){return a("div",{key:s,staticClass:"c-item"},[a("div",{staticClass:"c-icon kg-png",staticStyle:{"background-position":"-201px -140px",height:"16px"}}),a("p",{staticClass:"p1"},[t._v("Votes gap VS No."+t._s(s))]),a("p",{staticClass:"p2"},[t._v(t._s(e)+" ELA votes")])])}),0):t._e()]),a("div",{staticClass:"v-btn"},[a("mt-button",{staticClass:"cb",attrs:{size:"large",type:"primary"},on:{click:function(e){return t.clickVoteBtn()}}},[t._v("\n      "+t._s(t.$t("VOTE"))+"\n    ")])],1)])},X=[],tt=s("b311"),et=s.n(tt),st=null,at={data:function(){return{}},computed:{node_detail:function(){return this.$store.state.node_detail?(g.loading(!1),this.$store.state.node_detail):(g.loading(!0),null)}},mounted:function(){this.$store.dispatch("set_node_detail",{})},methods:{clickVoteBtn:function(){g.toastSuccess("click vote button")}},created:function(){st=new et.a(".c-copy"),st.on("success",function(t){g.toastInfo("copy successs"),console.log("copy text : "+t.text),t.clearSelection()})},destroyed:function(){st&&(st.destroy(),st=null)}},it=at,nt=(s("5628"),Object(C["a"])(it,Q,X,!1,null,null,null)),ct=nt.exports,ot=function(){var t=this,e=t.$createElement,s=t._self._c||e;return s("div",{staticClass:"kg-page p-myVoteDetail"},[t.data?s("div",{staticClass:"kg-body"},[s("div",{staticClass:"c-top"},[s("div",{staticClass:"c1"},[s("p",{staticClass:"p1"},[t._v(t._s(t.$t("VOTES"))+": (ELA)")]),s("p",{staticClass:"p2"},[t._v(t._s(t.data.number))])]),s("div",{staticClass:"c1"},[s("p",{staticClass:"p1"},[t._v(t._s(t.$t("NODES")))]),s("p",{staticClass:"p2"},[t._v(t._s(t.data.node))])])]),s("div",{staticClass:"c-mid"},[s("div",{staticClass:"kg-png"}),s("span",{staticClass:"s1"},[t._v(t._s(t.data.time))]),s("span",{staticClass:"s2"},[t._v(t._s(t.data.status))])]),s("div",{staticClass:"c-list",staticStyle:{"margin-top":"8px","padding-bottom":"60px"}},t._l(t.data.node_list,function(e,a){return s("VotingListItem",{key:a,attrs:{status:t.vote_status,clickFn:t.clickItem,item:e,index:a+1}})}),1)]):t._e(),s("div",{staticClass:"v-btn"},["list"===t.vote_status?s("mt-button",{staticClass:"cb",attrs:{size:"large",type:"primary"},on:{click:function(e){return t.clickVoteBtn1()}}},[t._v("\n      Re-Vote\n    ")]):t._e(),"vote"===t.vote_status?s("mt-button",{staticClass:"cb",attrs:{disabled:t.select.n<1,size:"large",type:"primary"},on:{click:function(e){return t.clickVoteBtn2()}}},[t._v("\n      Vote "),s("span",{staticStyle:{"font-size":"12px"}},[t._v(t._s("("+t.select.n+"/"+t.select.t+")"))])]):t._e()],1)])},lt=[],rt={components:{VotingListItem:E},data:function(){return{vote_status:"list",select:{n:0,t:0}}},computed:{data:function(){return this.$store.state.my_vote_detail?(g.loading(!1),this.$store.state.my_vote_detail.time=g.moment(this.$store.state.my_vote_detail.time).format("YYYY-MM-DD hh:mm"),this.$store.state.my_vote_detail):(g.loading(!0),null)}},mounted:function(){this.$store.dispatch("set_my_vote_detail",{})},methods:{clickItem:function(t){"list"===this.vote_status?this.$router.push("/node_detail/"+t.id):(t.selected=!t.selected,this.processSelectNumber())},clickVoteBtn1:function(){this.vote_status="vote",this.processSelectNumber()},clickVoteBtn2:function(){var t=this;g.toastSuccess("click vote btn"),g._.delay(function(){t.vote_status="list"},3e3)},processSelectNumber:function(){var t=g._.size(this.data.node_list),e=g._.size(g._.filter(this.data.node_list,function(t){return t.selected}));this.select={t:t,n:e}}}},dt=rt,ut=(s("3c7e"),Object(C["a"])(dt,ot,lt,!1,null,null,null)),ft=ut.exports;a["default"].use(n["a"]);var _t,pt=new n["a"]({routes:[{path:"/",name:"home",component:H},{path:"/node_detail/:id",name:"node_detail",component:ct},{path:"/vote_detail/:id",name:"vote_detail",component:ft}]}),vt=s("2f62"),mt=s("ade3"),bt=[{id:1,name:"Blockchain World",location:"Singapore",votes:"999888",fav:!0,percentage:66.6,reward:"26988",selected:!0,network_status:"online"},{id:2,name:"Blockchain World1",location:"Singapore",votes:"999888",fav:!1,percentage:24.39,reward:"26988",selected:!1,network_status:"offline"},{id:3,name:"Blockchain World",location:"Singapore",votes:"999888",fav:!0,percentage:66.6,reward:"26988",network_status:"away"},{id:4,name:"Blockchain World1",location:"Singapore",votes:"999888",fav:!1,percentage:24.39,reward:"26988",network_status:"online"},{id:5,name:"Blockchain World",location:"Singapore",votes:"999888",fav:!0,percentage:66.6,reward:"26988",network_status:"online"},{id:6,name:"Blockchain World1",location:"Singapore",votes:"999888",fav:!1,percentage:24.39,reward:"26988",network_status:"online"},{id:7,name:"Blockchain World",location:"Singapore",votes:"999888",fav:!0,percentage:66.6,reward:"26988",network_status:"online"},{id:8,name:"Blockchain World1",location:"Singapore",votes:"999888",fav:!1,percentage:24.39,reward:"26988",network_status:"online"},{id:9,name:"Blockchain World",location:"Singapore",votes:"999888",fav:!0,percentage:66.6,reward:"26988",network_status:"online"},{id:10,name:"Blockchain World1",location:"Singapore",votes:"999888",fav:!1,percentage:24.39,reward:"26988",network_status:"online"}],gt={node_list:bt,me_info:{vp_used:"200.4563",vp_total:"412.3456"},node_detail:{id:1,name:"Blockchain World",location:"Singapore",votes:"999888",fav:!0,percentage:66.6,reward:"26988",network_status:"online",status:"Active",rank:15,url:"http://www.baidu.com",public_key:"0x68asdfnskjfksdkjfks9989283",votes_gap:{24:"-112",96:"+323"}},my_votes_list:[{id:1,number:1200,time:Date.now(),node:18,status:"Confirming"},{id:2,number:100,time:Date.now(),node:12,status:"Success"},(_t={id:3,number:25,node:9},Object(mt["a"])(_t,"node",12),Object(mt["a"])(_t,"status","Failure"),_t)],my_vote_detail:{id:2,number:100,time:Date.now(),node:12,status:"Success",node_list:bt}},ht=gt,kt=s("bc3a"),jt=s.n(kt);jt.a.create({baseURL:"https://123.206.52.29/api/dposnoderpc"});a["default"].use(vt["a"]);var yt=new vt["a"].Store({state:{node_list:null,current_node:null,node_detail:null,my_votes_list:null,my_vote_detail:null,me_info:null,my_fav_list:null,current_tab:"tab1"},mutations:{set_node_list:function(t,e){t.node_list=g._.map(e,function(t){return g._.isUndefined(t.selected)&&(t.selected=!1),t})},set_current_node:function(t,e){t.current_node=e},set_node_detail:function(t,e){t.node_detail=e},set_my_votes_list:function(t,e){t.my_votes_list=e},set_me_info:function(t,e){t.me_info=e},set_my_vote_detail:function(t,e){e.node_list&&(e.node_list=g._.map(e.node_list,function(t){return g._.isUndefined(t.selected)&&(t.selected=!1),t})),console.log(11,e),t.my_vote_detail=e},set_my_fav_list:function(t,e){t.my_fav_list=g._.map(e,function(t){return g._.isUndefined(t.selected)&&(t.selected=!1),t})},set_tab:function(t,e){t.current_tab=e}},actions:{set_node_list:function(t,e){var s=t.commit;t.state;g.request(e,ht.node_list).then(function(t){s("set_node_list",t)})},set_node_detail:function(t,e){var s=t.commit;t.state;s("set_node_detail",null),g.request(e,ht.node_detail).then(function(t){s("set_node_detail",t)})},set_my_votes_list:function(t,e){var s=t.commit;g.request(e,ht.my_votes_list).then(function(t){s("set_my_votes_list",t)})},set_me_info:function(t,e){var s=t.commit;g.request(e,ht.me_info).then(function(t){s("set_me_info",t)})},set_my_vote_detail:function(t,e){var s=t.commit;g.request(e,ht.my_vote_detail).then(function(t){s("set_my_vote_detail",t)})},set_my_fav_list:function(t,e){var s=t.commit;g.request(e,ht.node_list).then(function(t){s("set_my_fav_list",t)})}}}),Ct=s("a925"),St={VOTING:"投票",RANK:"Rank",FAV:"Fav",LATEST:"Latest",NODES:"节点",FAVORITES:"最爱",MY_VOTES:"我的投票",NODE_DETAILS:"Node Details",VOTE:"Vote",VOTING_POWER_USED:"voting power Used",TOTAL:"Total",RULE:"Rule",ALL:"All",SUCCESS:"Success",FAILURE:"Failure",VOTING_RULE:"Voting Rule",rule_msg:{1:"1 ELA may be used to vote for a maximum of 36 different nodes and 1 ELA may only give the same node a maximum of 1 vote;",2:"After ELA has been used to cast votes (i.e., the vote has been successfully cast), the corresponding ELA will no longer be used in circulation. If ELA is transferred after being used for voting, then the original vote will naturally be revoked after transferring and there is no revoke period for revoking votes;"},MORE_DETAIL:"More Detail",RE_VOTE:"Re-Vote",VOTES:"Votes",STATUS:"状态",LOCATION:"位置"},Et={VOTING:"Voting",RANK:"Rank",FAV:"Fav",LATEST:"Latest",NODES:"Nodes",FAVORITES:"Favorites",MY_VOTES:"My Votes",NODE_DETAILS:"Node Details",VOTE:"Vote",VOTING_POWER_USED:"voting power Used",TOTAL:"Total",RULE:"Rule",ALL:"All",SUCCESS:"Success",FAILURE:"Failure",VOTING_RULE:"Voting Rule",rule_msg:{1:"1 ELA may be used to vote for a maximum of 36 different nodes and 1 ELA may only give the same node a maximum of 1 vote;",2:"After ELA has been used to cast votes (i.e., the vote has been successfully cast), the corresponding ELA will no longer be used in circulation. If ELA is transferred after being used for voting, then the original vote will naturally be revoked after transferring and there is no revoke period for revoking votes;"},MORE_DETAIL:"More Detail",RE_VOTE:"Re-Vote",VOTES:"Votes",STATUS:"Status",LOCATION:"Location"},wt={zh:St,en:Et},xt=wt;s("aa35"),s("1f54"),s("d940"),s("944d");a["default"].use(Ct["a"]),a["default"].use(u.a),a["default"].config.productionTip=!1;var Ot=new Ct["a"]({locale:"zh",messages:xt});window.changeLanguage=function(){var t=arguments.length>0&&void 0!==arguments[0]?arguments[0]:"en";Ot.locale=t},new a["default"]({router:pt,store:yt,i18n:Ot,render:function(t){return t(i["default"])}}).$mount("#app")},"5c0b":function(t,e,s){"use strict";var a=s("5e27"),i=s.n(a);i.a},"5e27":function(t,e,s){},"6c6e":function(t,e,s){},7025:function(t,e,s){},7938:function(t,e,s){"use strict";var a=s("1f96"),i=s.n(a);i.a},"7baa":function(t,e,s){},"944d":function(t,e,s){},af70:function(t,e,s){},b640:function(t,e,s){t.exports=s.p+"img/logo.a0747927.jpg"},bcc9:function(t,e,s){},d940:function(t,e,s){},e47c:function(t,e,s){"use strict";var a=s("6c6e"),i=s.n(a);i.a},ec0c:function(t,e,s){"use strict";var a=s("7025"),i=s.n(a);i.a}});
//# sourceMappingURL=app.8f4ff5dc.js.map