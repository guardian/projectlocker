import React from 'react';

class ShowPasswordComponent extends React.Component {
    render() {
        if(this.props.fieldName=='password') {
            let rendered="";
            for(let n=0;n<this.props.pass.length;++n){
                rendered+="*";
            }
            return (<span className="hidden-password">{rendered}</span>)
        } else {
            return <span>{this.props.pass}</span>;
        }
    }
}

export default ShowPasswordComponent;