import React from 'react';
import axios from 'axios';
import ShowPasswordComponent from '../ShowPasswordComponent.jsx';

class StorageCompletionComponent extends React.Component {
    constructor(props){
        super(props);

        this.state = {
            inProgress: false,
            error: null
        };
        this.confirmClicked = this.confirmClicked.bind(this);
    }

    requestContent(){
        const selectedStorage = this.props.strgTypes[this.props.selectedType];
        return {
            rootpath: this.props.subfolder,
            storageType: selectedStorage.name,
            host: this.props.loginDetails.hostname,
            port: this.props.loginDetails.port,
            user: this.props.loginDetails.username,
            password: this.props.loginDetails.password
        }
    }

    confirmClicked(event){
        this.setState({inProgress: true});
        axios.put("/storage",this.requestContent()).then(
            (response)=>{
                this.setState({inProgress: false});
                window.location = '/list/storages';
            }
        ).catch(
            (error)=>{
                this.setState({inProgress: false, error: error});
                console.error(error)
            }
        )
    }

    render() {
        const selectedStorage = this.props.strgTypes[this.props.selectedType];

        return(<div>
            <h3>Set up storage</h3>
            <p className="information">We will set up a new storage definition with the information below.</p>
            <p className="information">Press "Confirm" to go ahead, or press Previous if you need to amend any details.</p>
            <table>
                <tbody>
                <tr>
                    <td>Storage type</td>
                    <td>{selectedStorage.name}</td>
                </tr>
                <tr>
                    <td>Login details</td>
                    <td>
                        <ul>
                            {Object.entries(this.props.loginDetails).map((entry, index)=><li>
                                <span className="login-description">{entry[0]}: </span>
                                <span className="login-value"><ShowPasswordComponent pass={entry[1]} fieldName={entry[0]}/></span>
                            </li>)
                            }
                        </ul>
                    </td>
                </tr>
                <tr>
                    <td>Subfolder</td>
                    <td>{this.props.subfolder}</td>
                </tr>
                </tbody>
            </table>
            <span style={{float: "right"}}><button onClick={this.confirmClicked}>Confirm</button></span>
        </div>)
    }
}

export default StorageCompletionComponent;
