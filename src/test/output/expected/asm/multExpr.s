
.data

msg_0:
	.word 82
	.ascii	"OverflowError: the result is too small/large to store in a 4-byte signed-integer.\n"
msg_1:
	.word 3
	.ascii	"%d\0"
msg_2:
	.word 1
	.ascii	"\0"
msg_3:
	.word 5
	.ascii	"%.*s\0"

.text

.global main
main:
	PUSH {lr}
	SUB sp, sp, #8
	LDR r4, =5
	STR r4, [sp, #4]
	LDR r4, =3
	STR r4, [sp]
	LDR r4, [sp, #4]
	LDR r5, [sp]
	SMULL r4, r5, r4, r5
	CMP r5, r4, ASR #31
	BLNE p_throw_overflow_error
	MOV r0, r4
	BL p_print_int
	BL p_print_ln
	ADD sp, sp, #8
	LDR r0, =0
	POP {pc}
	.ltorg
p_throw_overflow_error:
	LDR r0, =msg_0
	BL p_throw_runtime_error
p_print_int:
	PUSH {lr}
	MOV r1, r0
	LDR r0, =msg_1
	ADD r0, r0, #4
	BL printf
	MOV r0, #0
	BL fflush
	POP {pc}
p_print_ln:
	PUSH {lr}
	LDR r0, =msg_2
	ADD r0, r0, #4
	BL puts
	MOV r0, #0
	BL fflush
	POP {pc}
p_throw_runtime_error:
	BL p_print_string
	MOV r0, #-1
	BL exit
p_print_string:
	PUSH {lr}
	LDR r1, [r0]
	ADD r2, r0, #4
	LDR r0, =msg_3
	ADD r0, r0, #4
	BL printf
	MOV r0, #0
	BL fflush
	POP {pc}
