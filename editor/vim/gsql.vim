" Vim syntax file
" Language: GSQL DDL
" Last Change:  2018 May 14
"
" NOTE: Add following three lines to vimrc
" au BufRead,BufNewFile *.gsql set filetype=gsql
" au! Syntax gsql source $VIM/syntax/gsql.vim
" syntax on
" """""""""""""""""""""""""""""""""""""""""""""""

" Those files usually have the extension  *.gsql

if exists("b:current_syntax")
  finish
endif

syntax case ignore

echom "gsql syntax highlighting code."

syn keyword basicType int float double bool string uint true false
syn keyword gsqlType OrAccum SumAccum MaxAccum MinAccum ListAccum MapAccum BagAccum SetAccum AvgAccum
syn keyword gsqlTodo contained TODO FIXME XXX NOTE

syn keyword gsqlKeywords create query for graph clear export vertex edge job loading online_post
syn keyword gsqlKeywords values to load using run with drop alter
syn keyword gsqlKeywords select from where accum order by limit having when case end else then
syn keyword gsqlKeywords print type delete upsert while if

syn match gsqlComment  "#.*$" contains=gsqlTodo
syn match gsqlComment  "//.*$" contains=gsqlTodo
syn match gsqlOperator "\v\+\="
syn match gsqlOperator "\v\="
syn match gsqlOperator "\v!\="
syn match gsqlOperator "\v\>\="
syn match gsqlOperator "\v\>"
syn match gsqlOperator "\v\<"
syn match gsqlOperator "\v\@"
syn match gsqlEdge     "-("
syn match gsqlEdge     ")-[>]\?"

syn match gsqlNumber   '\<\d\+\>'
syn match gsqlNumber   '\<[-+]\d\+\>'

" Floating point number with decimal no E or e (+,-)
syn match gsqlNumber   '\<\d\+\.\d*\>'
syn match gsqlNumber   '\<[-+]\d\+\.\d*\>'

" Floating point like number with E and no decimal point (+,-)
syn match gsqlNumber   '\<[-+]\=\d[[:digit:]]*[eE][\-+]\=\d\+\>'
syn match gsqlNumber   '\<\d[[:digit:]]*[eE][\-+]\=\d\+\>'

" Floating point like number with E and decimal point (+,-)
syn match gsqlNumber   '\<[-+]\=\d[[:digit:]]*\.\d*[eE][\-+]\=\d\+\>'
syn match gsqlNumber   '\<\d[[:digit:]]*\.\d*[eE][\-+]\=\d\+\>'


syn region gsqlComment start="/\*" end="\*/" contains=gsqlTodo
syn region cString     start=+L\="+ skip=+\\"+ end=+"+ contains=cSpecialChar

syn match cSpecialChar contained '\\[ntbf"\\]'


let b:current_syntax = "gsql"

hi def link gsqlTodo             Todo
hi def link gsqlKeywords         Keyword
hi def link gsqlType             Type
hi def link basicType            Type
hi def link gsqlComment          Comment
hi def link cString              String
hi def link gsqlOperator         Operator
hi def link gsqlEdge             Operator
hi def link gsqlNumber           Constant
hi def link cSpecialChar         Special


"hi def link gsqlBlockCmd         Statement
"hi def link gsqlHip              Type
"hi def link gsqlDesc             PreProc
