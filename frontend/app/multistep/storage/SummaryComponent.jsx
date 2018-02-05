import React from 'react';
import ShowPasswordComponent from '../ShowPasswordComponent.jsx';

class SummaryComponent extends React.Component {
    constructor(props){
        super(props);
    }

    render() {
        const selectedStorage = this.props.selectedStorage;
        return <table>
            <tbody>
            <tr>
                <td>Storage type</td>
                <td id="storageType">{this.props.name}</td>
            </tr>
            <tr>
                <td>Login details</td>
                <td id="storageLoginDetails">
                    <ul>
                        {Object.entries(this.props.loginDetails).map((entry, index)=>entry[0] && entry[1] ? <li key={index}>
                            <span className="login-description">{entry[0]}: </span>
                            <span className="login-value"><ShowPasswordComponent pass={entry[1]} fieldName={entry[0]}/></span>
                        </li> : <li key={index}/>)
                        }
                    </ul>
                </td>
            </tr>
            <tr>
                <td>Subfolder</td>
                <td className={this.props.subfolder ? "" : "value-not-present"} id="storageSubfolder">{this.props.subfolder ? this.props.subfolder : "(none)"}</td>
            </tr>
            </tbody>
        </table>;
    }
}

export default SummaryComponent;