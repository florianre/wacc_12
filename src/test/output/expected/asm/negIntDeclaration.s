
.text

.global main
main:
	PUSH {lr}
	SUB sp, sp, #4
	LDR r4, =-1
	STR r4, [sp]
	ADD sp, sp, #4
	LDR r0, =0
	POP {pc}
	.ltorg
