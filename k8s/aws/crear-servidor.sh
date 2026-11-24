#!/bin/bash
# Crea el servidor de veterinarIA en AWS. Pensado para AWS CloudShell (ya tiene el CLI y tus permisos):
#   curl -sO https://raw.githubusercontent.com/Umbrabilis/veterinarIA-API/<rama>/k8s/aws/crear-servidor.sh
#   bash crear-servidor.sh
# Crea: un grupo de seguridad (HTTP abierto; SSH solo desde EC2 Instance Connect de la consola) y una instancia
# Ubuntu 24.04 con 30 GB de disco que se instala sola con k8s/aws/instalar-servidor.sh.
# Tipo por defecto m7i-flex.large (2 vCPU, 8 GB): elegible en el plan gratuito con créditos.
set -euo pipefail

TIPO="${TIPO:-m7i-flex.large}"
RAMA="${RAMA:-feature/notificaciones-graphql-k8s}"
NOMBRE=veterinaria
REGION="$(aws configure get region || echo "${AWS_REGION:-us-east-2}")"

VPC=$(aws ec2 describe-vpcs --filters Name=isDefault,Values=true --query 'Vpcs[0].VpcId' --output text)
AMI=$(aws ssm get-parameter --name /aws/service/canonical/ubuntu/server/24.04/stable/current/amd64/hvm/ebs-gp3/ami-id \
    --query Parameter.Value --output text)

SG=$(aws ec2 describe-security-groups --filters Name=group-name,Values=$NOMBRE Name=vpc-id,Values="$VPC" \
    --query 'SecurityGroups[0].GroupId' --output text)
if [ "$SG" = "None" ]; then
    SG=$(aws ec2 create-security-group --group-name $NOMBRE --vpc-id "$VPC" \
        --description "veterinarIA: HTTP publico y SSH solo desde EC2 Instance Connect" --query GroupId --output text)
    aws ec2 authorize-security-group-ingress --group-id "$SG" --protocol tcp --port 80 --cidr 0.0.0.0/0 >/dev/null
    PL=$(aws ec2 describe-managed-prefix-lists \
        --filters Name=prefix-list-name,Values="com.amazonaws.$REGION.ec2-instance-connect" \
        --query 'PrefixLists[0].PrefixListId' --output text)
    aws ec2 authorize-security-group-ingress --group-id "$SG" \
        --ip-permissions "IpProtocol=tcp,FromPort=22,ToPort=22,PrefixListIds=[{PrefixListId=$PL}]" >/dev/null
fi

curl -sf "https://raw.githubusercontent.com/Umbrabilis/veterinarIA-API/$RAMA/k8s/aws/instalar-servidor.sh" -o /tmp/instalar.sh

INSTANCIA=$(aws ec2 run-instances --image-id "$AMI" --instance-type "$TIPO" --security-group-ids "$SG" \
    --block-device-mappings '[{"DeviceName":"/dev/sda1","Ebs":{"VolumeSize":30,"VolumeType":"gp3"}}]' \
    --user-data file:///tmp/instalar.sh \
    --metadata-options HttpTokens=required \
    --tag-specifications "ResourceType=instance,Tags=[{Key=Name,Value=$NOMBRE}]" \
    --query 'Instances[0].InstanceId' --output text)
echo "Instancia: $INSTANCIA ($TIPO). Esperando a que arranque..."
aws ec2 wait instance-running --instance-ids "$INSTANCIA"
IP=$(aws ec2 describe-instances --instance-ids "$INSTANCIA" --query 'Reservations[0].Instances[0].PublicIpAddress' --output text)
echo "IP pública: $IP"
echo "La instalación tarda unos 25 minutos. Progreso:"
echo "  aws ec2 get-console-output --instance-id $INSTANCIA --latest --output text | grep VETERINARIA"
echo "Aplicación: http://$IP/"
