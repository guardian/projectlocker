import axios from "axios";
import React from 'react';

class CommonCompletionComponent extends React.Component {
    constructor(props){
        super(props);
        this.endpoint = "/api/unknown"; // override this to the api endpoint that you want to hit
        this.successRedirect = "/unknown/";    //override this to the page to go to when successfully saved

        this.confirmClicked = this.confirmClicked.bind(this);
    }

    /* this method should create a json object from the component's state and should be implemented in your subclass */
    requestContent(){
        throw "Not implemented";
    }

    /* you can override this method to do something before the page gets redirected.
    This should return a Promise - if the promise rejects, then the page does not get redirected and the error returned
     should be displayed as the component error. If the promise accepts, then the page is redirected to `this.successRedirect`. */
    recordDidSave(){
        return new Promise((accept, reject)=>accept());
    }

    /*confirmClicked handler to send data to the server*/
    confirmClicked(event){
        this.setState({inProgress: true});
        const endpoint = this.props.currentEntry ? this.endpoint + "/" + this.props.currentEntry : this.endpoint;

        axios.request({method: "PUT", url: endpoint,data: this.requestContent()}).then(
            (response)=>{
                this.setState({inProgress: false}, ()=>{
                    this.recordDidSave()
                        .then(()=>{
                            console.log("save succeeded");
                            window.location.assign(this.successRedirect)
                        });    //rely on catch() below to log errors
                });
            }
        ).catch(
            (error)=>{
                this.setState({inProgress: false, error: error});
                console.error(error)
            }
        )
    }
}

export default CommonCompletionComponent;